package com.example.scraper.service.scraper.playwright.immobilier;

import com.example.scraper.model.ImmobilierProperty;
import com.example.scraper.service.scraper.playwright.AbstractPlaywrightScraper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.TimeoutError;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service("playwrightImmobilierScraper")
@Slf4j
public class ImmobilierScraper extends AbstractPlaywrightScraper<ImmobilierProperty> {

    private static final Logger log = LoggerFactory.getLogger(ImmobilierScraper.class);
    private static final String BASE_URL = "https://www.immobilier.com.tn/";
    private static final Pattern ID_PATTERN = Pattern.compile("Location #(\\d+)");
    
    @Autowired
    private ApplicationContext applicationContext;

    // Selectors for phone input fields
    private static final String[] phoneInputSelectors = new String[] {
        "input[name='phone']", 
        "input[type='tel']", 
        "input[placeholder*='phone']", 
        "input[placeholder*='téléphone']",
        "input.phone-input",
        ".modal input[type='tel']",
        ".modal input[name='phone']"
    };
    
    private static final String[] submitButtonSelectors = new String[] {
        "button[type='submit']",
        "button.submit-btn",
        "button.btn-submit",
        "button.btn-primary",
        "button.submit",
        "input[type='submit']",
        ".modal button[type='submit']",
        ".modal .btn-primary",
        "button:has-text('Accéder au numéro')",
        ".btn-phone", "button:contains('Phone')", "button:contains('Numéro')"
    };
    
    // Selectors for description content
    private static final String[] descriptionSelectors = new String[] {
        ".property-description",
        ".description",
        ".property-details",
        ".details",
        ".content",
        ".property-content",
        ".annonce-texte",
        ".property-text",
        ".property-info",
        ".property-card p"
    };

    @Override
    public List<ImmobilierProperty> scrape(String url) {
        log.info("Starting scraping process for immobilier.com.tn");
        
        String targetUrl = url;
        if (url == null || url.isEmpty() || !url.contains("immobilier.com.tn")) {
            targetUrl = BASE_URL;
        }
        
        return super.scrape(targetUrl);
    }

    @Override
    protected List<ImmobilierProperty> extractData(Page page) {
        log.info("Starting extraction from immobilier.com.tn");
        List<ImmobilierProperty> properties = new ArrayList<>();

        try {
            String searchUrl = "https://www.immobilier.com.tn/resultat-recherche?ta=2&r=0&tb=3&pcs=&smi=&pma=800&page=1";
            
            log.info("Navigating directly to search URL: {}", searchUrl);
            page.navigate(searchUrl);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get("./immobilier-search.png")));
            log.info("Screenshot saved to ./immobilier-search.png");

            List<ElementHandle> listingCards = page.querySelectorAll(".annonce-card");
            log.info("Found {} listing cards on the page", listingCards.size());
            
            int count = 0;
            List<String> processedUrls = new ArrayList<>();
            
            for (ElementHandle card : listingCards) {
                try {
                    String href = card.getAttribute("href");
                    
                    if (href != null && href.contains("/annonce/")) {
                        
                        if (!href.startsWith("http")) {
                            href = BASE_URL + (href.startsWith("/") ? href.substring(1) : href);
                        }
                        
                        if (processedUrls.contains(href)) {
                             log.info("Skipping already processed URL: {}", href);
                            continue;
                        }
                        
                        if (href.contains("/connexion") || href.contains("/login") || 
                            href.contains("/soumettre") || href.contains("/inscription")) {
                            log.info("Skipping non-listing page URL: {}", href);
                            continue;
                        }
                        
                        processedUrls.add(href);
                        log.info("Processing listing link: {}", href);
                        
                        ImmobilierProperty property = new ImmobilierProperty();
                        property.setUrl(href);
                        property.setSourceWebsite("immobilier.com.tn");

                        String initialPriceText = "";
                        boolean initialIsPriceOnRequest = false;
                        try {
                            ElementHandle priceElement = card.querySelector(".price.price-location span");
                             if (priceElement != null) {
                                 if (priceElement.getAttribute("class") != null && priceElement.getAttribute("class").contains("no-price")) {
                                     initialIsPriceOnRequest = true;
                                     initialPriceText = "0";
                                     log.info("Found 'Prix sur demande' on search results card for {}", href);
                                 } else {
                                     initialPriceText = priceElement.textContent().trim();
                                     log.info("Found price text on search results card for {}: {}", href, initialPriceText);
                                     Pattern pricePattern = Pattern.compile("(\\d+)\\s*(?:DT|TND|dt|Dt|tnd)?", Pattern.CASE_INSENSITIVE);
                                     Matcher priceMatcher = pricePattern.matcher(initialPriceText);
                                     if (priceMatcher.find()) {
                                         initialPriceText = priceMatcher.group(1).trim();
                                         log.info("Cleaned initial price from card: {}", initialPriceText);
                                     } else {
                                          log.warn("Could not extract numeric price from card text '{}' for {}", initialPriceText, href);
                                          initialPriceText = "";
                                     }
                                 }
                             } else {
                                 log.warn("Could not find price span element within card for {}", href);
                             }
                             
                             if (!initialPriceText.isEmpty()) {
                                property.setPrice(initialPriceText);
                                log.info("Set initial price for property {}: {}", href, property.getPrice());
                             } else {
                                property.setPrice("0");
                                log.info("Defaulting price to 0 for property {} as none found on card.", href);
                             }
                             
                        } catch (Exception priceEx) {
                             log.error("Error extracting initial price from card for {}: {}", href, priceEx.getMessage());
                             property.setPrice("0");
                        }

                        Pattern idPatternLocal = Pattern.compile("/annonce/(\\d+)/");
                        Matcher idMatcherLocal = idPatternLocal.matcher(href);
                        if (idMatcherLocal.find()) {
                            property.setId(idMatcherLocal.group(1));
                            log.info("Extracted ID: {}", property.getId());
                        } else {
                             log.warn("Could not extract ID from URL: {}", href);
                        }
                        
                        Page detailPage = page.context().newPage();
                        try {
                            log.info("Opening detail page for property ID {}: {}", property.getId(), href);
                            detailPage.navigate(href);
                            detailPage.waitForLoadState(LoadState.NETWORKIDLE);
                            
                            ElementHandle titleElement = detailPage.querySelector("h1");
                            if (titleElement != null) {
                                property.setTitle(titleElement.textContent().trim());
                                log.info("Found title on detail page: {}", property.getTitle());
                                
                                if (property.getTitle() != null) {
                                    Pattern roomPatternLocal = Pattern.compile("S\\+?(\\d+)", Pattern.CASE_INSENSITIVE);
                                    Matcher roomMatcherLocal = roomPatternLocal.matcher(property.getTitle());
                                    if (roomMatcherLocal.find()) {
                                        try {
                                            int rooms = Integer.parseInt(roomMatcherLocal.group(1)) + 1;
                                            property.setRooms(rooms);
                                            log.info("Parsed room count from title (S+N pattern): {}", rooms);
                                        } catch (NumberFormatException e) {
                                            log.debug("Could not parse room count from title S+ pattern");
                                        }
                                    }
                                }
                            } else {
                                 log.warn("Could not find title (h1) on detail page: {}", href);
                            }
                            
                            String description = extractDescription(detailPage);
                            if (description != null && !description.isEmpty()) {
                                property.setDescription(description);
                                log.info("Found description on detail page (Length: {})", description.length());
                                if (property.getRooms() == null) {
                                    extractRoomsFromText(property, description);
                                }
                            } else {
                                 log.warn("Could not extract description from detail page: {}", href);
                            }
                             
                            log.info("Using price extracted from search results card: {}", property.getPrice());

                            String locationText = "";
                             if (property.getTitle() != null && property.getTitle().contains("\n")) {
                                 String[] parts = property.getTitle().split("\n", 2);
                                 if (parts.length > 1) {
                                     locationText = parts[1].trim();
                                 } else {
                                     locationText = property.getTitle().trim();
                                 }
                             } else {
                                 ElementHandle locationElement = detailPage.querySelector(".property-location, .address, .location-info small");
                                 if (locationElement != null) {
                                     locationText = locationElement.textContent().trim();
                                 } else if (property.getTitle() != null) {
                                     locationText = property.getTitle().trim();
                                 }
                             }
                             
                            property.setLocation(locationText);
                            log.info("Set raw location string: {}", locationText);

                            if (!locationText.isEmpty()) {
                                String[] locationElements = locationText.split(",");
                                if (locationElements.length > 0) {
                                    property.setDistrict(locationElements[0].trim());
                                    log.info("Parsed district: {}", property.getDistrict());
                                }
                                if (locationElements.length > 1) {
                                    property.setCity(locationElements[1].trim());
                                     log.info("Parsed city: {}", property.getCity());
                                } else if (property.getDistrict() != null && !property.getDistrict().toLowerCase().contains("tunis")) {
                                    property.setCity(property.getDistrict());
                                    log.info("Set city same as district: {}", property.getCity());
                                }
                            }
                            
                            String fullAddress = buildCleanAddressForApi(property);
                            if (fullAddress == null) {
                                 StringBuilder addressBuilder = new StringBuilder();
                                 if (property.getDistrict() != null && !property.getDistrict().isEmpty()) addressBuilder.append(property.getDistrict());
                                 if (property.getCity() != null && !property.getCity().isEmpty()) {
                                     if (addressBuilder.length() > 0 && !property.getCity().equalsIgnoreCase(property.getDistrict())) addressBuilder.append(", ");
                                     if (!property.getCity().equalsIgnoreCase(property.getDistrict())) addressBuilder.append(property.getCity());
                                 }
                                 if (addressBuilder.length() > 0) addressBuilder.append(", Tunisia");
                                 fullAddress = addressBuilder.toString();
                            }
                            property.setFullAddress(fullAddress);
                            log.info("Built full address: {}", fullAddress);
                            
                            // --- Attempt to extract coordinates directly from page script ---
                            try {
                                String pageContent = detailPage.content(); // Get full HTML content
                                Pattern leafletPattern = Pattern.compile("initLeaflet\\s*\\(\\s*['\"]map-canvas['\"]\\s*,\\s*([-+]?\\d*\\.?\\d+)\\s*,\\s*([-+]?\\d*\\.?\\d+)\\s*\\)");
                                Matcher leafletMatcher = leafletPattern.matcher(pageContent);
                                
                                if (leafletMatcher.find()) {
                                    double lat = Double.parseDouble(leafletMatcher.group(1));
                                    double lng = Double.parseDouble(leafletMatcher.group(2));
                                    property.setLatitude(lat);
                                    property.setLongitude(lng);
                                    log.info("Successfully extracted coordinates from initLeaflet script: {}, {}", lat, lng);
                                    // Also set formatted address to indicate source if geocodeProperty is skipped
                                    property.setFormattedAddress("Coordinates from page script"); 
                                } else {
                                    log.warn("Could not find initLeaflet script pattern on page: {}", href);
                                }
                            } catch (Exception scriptEx) {
                                log.error("Error extracting coordinates from page script for {}: {}", href, scriptEx.getMessage());
                            }
                            // --- End coordinate extraction from script ---
                            
                            geocodeProperty(property);

                            // --- Start Room & Bedroom Extraction ---
                            Integer extractedRooms = null;
                            // Priority 1: Look for puzzle piece element
                            try {
                                Locator puzzleLocator = detailPage.locator("li:has(i.fa-puzzle-piece)");
                                if (puzzleLocator.count() > 0) {
                                    String puzzleText = puzzleLocator.first().textContent().trim();
                                    Matcher puzzleMatcher = Pattern.compile("\\d+").matcher(puzzleText);
                                    if (puzzleMatcher.find()) {
                                        extractedRooms = Integer.parseInt(puzzleMatcher.group());
                                        log.info("Extracted room count ({}) from puzzle piece element.", extractedRooms);
                                        property.setRooms(extractedRooms);
                                    }
                                }
                            } catch (Exception pEx) {
                                log.warn("Error trying to extract rooms from puzzle piece: {}", pEx.getMessage());
                            }

                            // Priority 2: Fallback to text patterns if puzzle piece not found/parsed
                            if (extractedRooms == null) {
                                log.info("Puzzle piece rooms not found, trying text patterns...");
                                if (property.getTitle() != null) {
                                    extractRoomsFromText(property, property.getTitle()); // Check title first
                                }
                                // Check description only if title didn't provide rooms
                                if (property.getRooms() == null && property.getDescription() != null) {
                                    extractRoomsFromText(property, property.getDescription());
                                }
                            }

                            // Calculate Bedrooms based on final Room count
                            if (property.getRooms() != null && property.getRooms() > 0) {
                                int bedrooms = property.getRooms() - 1;
                                property.setBedrooms(bedrooms);
                                log.info("Calculated bedrooms: {}", bedrooms);
                            } else if (property.getRooms() != null && property.getRooms() == 1) {
                                // Handle Studio case (1 room, 0 bedrooms)
                                property.setBedrooms(0);
                                log.info("Set bedrooms to 0 for studio/single room property.");
                            }
                            // --- End Room & Bedroom Extraction ---

                            // --- Start Phone Number Logic ---
                            String phoneNumber = extractPhoneNumber(detailPage, property);
                            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                                property.setContactPhone(phoneNumber);
                                log.info("Extracted phone number directly from page: {}", phoneNumber);
                            } else {
                                log.info("Phone number not found directly, attempting interaction...");
                                // 2. If not found directly, interact with the phone reveal modal
                                try {
                                    ElementHandle showPhoneButton = detailPage.querySelector("a[data-target='#phoneModal']:has-text('Afficher le numéro')");
                                    if (showPhoneButton != null) {
                                        log.info("Found 'Afficher le numéro' button, clicking...");
                                        showPhoneButton.click();

                                        // Wait for the modal to appear
                                        try {
                                            detailPage.waitForSelector("#phoneModal", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
                                            log.info("Phone modal is visible.");

                                            // Find and fill the input field using its specific ID
                                            Locator phoneInputLocator = detailPage.locator("#prospectPhone");
                                            // Find the submit button in the footer
                                            Locator submitButton = detailPage.locator("#phoneModal .modal-footer button:has-text('Accéder au numéro')");

                                            if (phoneInputLocator.isVisible()) { // Check only for input visibility initially
                                                log.info("Modal input field found. Interacting...");
                                                phoneInputLocator.fill("29867984"); // Using the example number
                                                phoneInputLocator.press("Enter"); // Trigger event to enable button
                                                log.info("Filled input and triggered Enter key press.");

                                                // Wait for button to be visible and enabled before clicking
                                                try {
                                                    log.info("Waiting for submit button to be visible and enabled...");
                                                    // Ensure button exists before waiting for state
                                                    submitButton.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(2000));
                                                    // Check if button is enabled (not disabled)
                                                    boolean isEnabled = !submitButton.isDisabled();
                                                    if (!isEnabled) {
                                                        log.info("Submit button is visible but disabled, waiting up to 3s for it to become enabled...");
                                                        // Additional wait to see if button becomes enabled
                                                        detailPage.waitForTimeout(3000);
                                                        isEnabled = !submitButton.isDisabled();
                                                    }

                                                    if (isEnabled) {
                                                        log.info("Submit button is enabled, clicking...");
                                                        submitButton.click();
                                                        log.info("Clicked 'Accéder au numéro'. Waiting for number reveal...");

                                                        // Wait for the phone number link to appear in the modal body
                                                        Locator revealedPhoneLink = detailPage.locator("#phoneModal .modal-body a[href^='tel:']");
                                                        revealedPhoneLink.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000)); // Wait up to 5s
                                                        log.info("Revealed phone link appeared in modal body.");

                                                        // 3. Extract phone number from the link's href
                                                        String telHref = revealedPhoneLink.getAttribute("href");
                                                        if (telHref != null && telHref.startsWith("tel:")) {
                                                            String revealedPhoneNumber = telHref.substring(4).replaceAll("[^\\d/]", ""); // Extract digits and slash only

                                                            // Additional cleanup/validation if needed (using the updated regex)
                                                            final Pattern phonePattern = Pattern.compile("(?:\\+216\\s?)?(\\d{8}(?:\\s*\\/\\s*\\d{8})?)");
                                                            Matcher phoneMatcherVar = phonePattern.matcher(revealedPhoneNumber);
                                                            if (phoneMatcherVar.find()) {
                                                                revealedPhoneNumber = phoneMatcherVar.group(1); // Get the core number part
                                                                property.setContactPhone(revealedPhoneNumber);
                                                                log.info("Successfully extracted phone number from modal interaction: {}", revealedPhoneNumber);
                                                            } else {
                                                                log.warn("Extracted href '/{} /' did not match expected phone pattern after cleanup.", revealedPhoneNumber);
                                                            }
                                                        } else {
                                                            log.warn("Could not extract href or href did not start with 'tel:' from revealed link.");
                                                        }
                                                    }
                                                } catch (TimeoutError e) {
                                                    log.warn("Submit button did not become visible/enabled or revealed phone link did not appear within timeout: {}", e.getMessage());
                                                }
                                            } else {
                                                log.warn("Could not find phone input #prospectPhone in the modal.");
                                            }
                                        } catch (TimeoutError e) {
                                            log.warn("Phone modal did not appear or timed out.");
                                        }

                                        // 4. Fallback: If interaction failed to get number, try general extraction again
                                        // Ensure we only run fallback if property phone is still null/empty after interaction attempt
                                        if (property != null && (property.getContactPhone() == null || property.getContactPhone().isEmpty())) {
                                            log.info("Falling back to general page scan after interaction attempt.");
                                            String fallbackNumber = extractPhoneNumber(detailPage, property); // Use a separate variable
                                            if (fallbackNumber != null && !fallbackNumber.isEmpty()) {
                                                property.setContactPhone(fallbackNumber);
                                                log.info("Successfully extracted phone number via fallback scan: {}", fallbackNumber);
                                            } else {
                                                log.warn("Fallback scan also failed to find phone number after interaction.");
                                            }
                                        }
                                    } else {
                                        log.info("'Afficher le numéro' button not found. Cannot attempt interaction.");
                                    }
                                } catch (Exception e) {
                                    log.warn("Error during phone number modal interaction: {}", e.getMessage());
                                }
                            }
                            // --- End Phone Number Logic ---
                            
                            String surfaceText = "";
                             // Use standard Playwright locators instead of :has and :contains
                             Locator surfaceLocator = detailPage.locator(".amenities li").filter(new Locator.FilterOptions().setHas(detailPage.locator("i.icon-area")))
                                                           .or(detailPage.locator(".details li:text('Surface')")) // Combine with .or()
                                                           .or(detailPage.locator(".features li:contains('Surface')"))
                                                           .first(); // Get the first matching element
                             
                             try {
                                 if (surfaceLocator.count() > 0) { // Check if the locator found anything
                                    surfaceText = surfaceLocator.textContent().trim();
                                    log.info("Found surface text element: {}", surfaceText);
                                    Matcher surfaceMatcher = Pattern.compile("(\\d+)\\s*(m|m²|m2)", Pattern.CASE_INSENSITIVE).matcher(surfaceText);
                                     if (surfaceMatcher.find()) {
                                         property.setSurface(surfaceMatcher.group(1) + " m²"); // Standardize
                                         log.info("Parsed surface area: {}", property.getSurface());
                                     } else {
                                          log.warn("Could not parse numeric surface area from text: {}", surfaceText);
                                     }
                                 } else {
                                     // Fallback to regex search on body if specific element fails
                                     log.info("Specific surface element not found, falling back to body search for {}", href);
                                     surfaceText = detailPage.evaluate("() => {" +
                                         "  const text = document.body.innerText;" +
                                         "  const surfaceMatch = text.match(/(\\d+)\\s*m[²²2]/);" +
                                         "  return surfaceMatch ? surfaceMatch[0] : '';" +
                                         "}").toString();
                                     if (!surfaceText.isEmpty()) {
                                         property.setSurface(surfaceText.replaceFirst("m[²²2]", " m²").trim()); // Clean and standardize
                                         log.info("Found surface area via body regex: {}", property.getSurface());
                                     } else {
                                         log.warn("Could not extract surface area for {}", href);
                                     }
                                 }
                             } catch (Exception surfEx) {
                                 log.error("Error extracting surface area for {}: {}", href, surfEx.getMessage());
                             }
                            
                            // --- Extract Images from Slider or Specific Elements ---
                            property.getImageUrls().clear(); // Clear any previously added incorrect images
                            boolean imagesFound = false;

                            // Priority 1: Try the slider structure
                            List<ElementHandle> sliderImageLinks = detailPage.querySelectorAll(".slider-for > a.fancybox");
                            if (sliderImageLinks != null && !sliderImageLinks.isEmpty()) {
                                log.info("Found {} images in slider structure.", sliderImageLinks.size());
                                for (ElementHandle imgLink : sliderImageLinks) {
                                    String imgSrc = imgLink.getAttribute("href");
                                    if (imgSrc != null && !imgSrc.isEmpty()) {
                                        imgSrc = resolveImageUrl(imgSrc, BASE_URL);
                                        if (imgSrc != null) {
                                            if (property.getMainImageUrl() == null) {
                                                property.setMainImageUrl(imgSrc);
                                                log.info("Set main image URL from slider: {}", imgSrc);
                                            }
                                            property.getImageUrls().add(imgSrc);
                                            imagesFound = true;
                                        }
                                    }
                                }
                            } else {
                                log.info("Slider structure (.slider-for > a.fancybox) not found.");
                            }

                            // Priority 2: Fallback to looking for common single image elements if slider failed
                            if (!imagesFound) {
                                log.info("Falling back to searching for common single image elements.");
                                String[] singleImageSelectors = { 
                                    ".property-image img", ".listing-image img", ".main-image img", 
                                    ".property-details img", ".details img", ".image img" 
                                };
                                List<ElementHandle> singleImages = detailPage.querySelectorAll(String.join(", ", singleImageSelectors));
                                
                                if (singleImages != null && !singleImages.isEmpty()) {
                                     log.info("Found {} potential single images using fallback selectors.", singleImages.size());
                                    for (ElementHandle imgEl : singleImages) {
                                        String imgSrc = imgEl.getAttribute("src");
                                        // Add extra filtering to avoid logos, thumbnails, icons etc.
                                        if (imgSrc != null && !imgSrc.isEmpty() && 
                                            !imgSrc.contains("logo") && !imgSrc.contains("avatar") && 
                                            !imgSrc.contains("nothumb") && !imgSrc.contains("icon") && 
                                            !imgSrc.contains("/points/") && // Explicitly exclude map points
                                            (imgSrc.contains("annonce") || imgSrc.contains("property") || imgSrc.contains("image") || imgSrc.contains("uploads"))) 
                                        {
                                            imgSrc = resolveImageUrl(imgSrc, BASE_URL);
                                            if (imgSrc != null) {
                                                if (property.getMainImageUrl() == null) {
                                                    property.setMainImageUrl(imgSrc);
                                                     log.info("Set main image URL from fallback selector: {}", imgSrc);
                                                }
                                                property.getImageUrls().add(imgSrc);
                                                 imagesFound = true;
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // If still no specific images found, log a warning
                             if (!imagesFound) {
                                 log.warn("Could not find specific property images for {}", href);
                                 // Optionally grab the first generic image as a last resort, but might be wrong
                                 // ElementHandle firstImg = detailPage.querySelector("img");
                                 // if (firstImg != null) property.getImageUrls().add(resolveImageUrl(firstImg.getAttribute("src"), BASE_URL));
                             }

                             log.info("Extracted {} image URLs for {}", property.getImageUrls().size(), href);
                            
                            log.info("Completed extraction for property ID {}: {}", property.getId(), property);
                            properties.add(property);
                            
                            count++;
                            if (count >= 50) {
                                log.info("Reached scrape limit of 50 properties.");
                                break;
                            }
                            
                        } catch (Exception detailEx) {
                             log.error("Error processing detail page {} for property ID {}: {}", href, property.getId(), detailEx.getMessage(), detailEx);
                        } finally {
                            if (detailPage != null && !detailPage.isClosed()) {
                                detailPage.close();
                            }
                        }
                    } else {
                    }
                } catch (Exception cardEx) {
                    log.error("Error processing a listing card: {}", cardEx.getMessage(), cardEx);
                }
            }
            
            if (count >= 50) {
                log.info("Exiting extraction early due to 50 property limit.");
            }
            
            log.info("Total properties successfully processed: {}", properties.size());
            
        } catch (Exception e) {
            log.error("Error scraping immobilier.com.tn: {}", e.getMessage(), e);
        }
        
        return properties;
    }
    
    /**
     * Extract description from the detail page using various selectors
     */
    private String extractDescription(Page page) {
        String description = "";
        
        for (String selector : descriptionSelectors) {
            try {
                ElementHandle descElement = page.querySelector(selector);
                if (descElement != null) {
                    String text = descElement.textContent().trim();
                    if (text.length() > 20) {  // Meaningful description
                        log.info("Found description using selector {}", selector);
                        return limitDescription(text);
                    }
                }
            } catch (Exception e) {
                log.debug("Error with selector {}: {}", selector, e.getMessage());
            }
        }
        
        // If no specific element found, try getting all paragraph text
        try {
            List<ElementHandle> paragraphs = page.querySelectorAll("p");
            StringBuilder sb = new StringBuilder();
            
            for (ElementHandle p : paragraphs) {
                String text = p.textContent().trim();
                if (text.length() > 20) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(text);
                    
                    // Limit the total description length
                    if (sb.length() > 1000) {
                        break;
                    }
                }
            }
            
            if (sb.length() > 0) {
                return limitDescription(sb.toString());
            }
        } catch (Exception e) {
            log.debug("Error extracting paragraphs: {}", e.getMessage());
        }
        
        // Last resort: Extract main text content from the page body
        try {
            String bodyText = page.evaluate("() => {\n" +
                "  const el = document.querySelector('.property-content, .property-description, .description, .details');\n" +
                "  return el ? el.innerText : document.body.innerText;\n" +
                "}").toString();
            
            if (bodyText.length() > 100) {
                // Try to extract a meaningful section
                String[] sentences = bodyText.split("\\.");
                StringBuilder meaningful = new StringBuilder();
                
                for (String sentence : sentences) {
                    if (sentence.length() > 15 && !sentence.contains("cookie") && 
                        !sentence.contains("copyright") && !sentence.contains("navigation")) {
                        if (meaningful.length() > 0) {
                            meaningful.append(". ");
                        }
                        meaningful.append(sentence.trim());
                        
                        if (meaningful.length() > 300) {
                            break;
                        }
                    }
                }
                
                if (meaningful.length() > 0) {
                    return limitDescription(meaningful.toString());
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting body text: {}", e.getMessage());
        }
        
        return description;
    }
    
    /**
     * Limit description to a reasonable length
     */
    private String limitDescription(String description) {
        final int MAX_DESCRIPTION_LENGTH = 2000;
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            return description.substring(0, MAX_DESCRIPTION_LENGTH) + "...";
        }
        return description;
    }
    
    /**
     * Extract phone number from the detail page
     * @param page The Playwright page
     * @param property The property object to update with phone info
     * @return The extracted phone number or null if not found
     */
    private String extractPhoneNumber(Page page, ImmobilierProperty property) {
        try {
            log.info("Attempting to extract phone number from page URL: {}", page.url());

            // --- Step 1: Check direct phone elements first ---
            String directPhone = page.evaluate("() => {" +
                 "  const phoneSelectors = [ '.phone', '.contact-phone', '.phone-number', '.tel', '[itemprop=telephone]', '.contact-info .phone', '.contact-details .phone', '.contact .phone', 'a[href^=\"tel:\"]' ];" +
                 "  for (const selector of phoneSelectors) { " +
                 "    const el = document.querySelector(selector); " +
                 "    if (el) { " +
                 "      const phone = el.innerText || el.getAttribute('href')?.replace('tel:', ''); " +
                 "      if (phone && phone.match(/\\d{8}/)) return phone.match(/\\d{8}/)[0]; " +
                 "    } " +
                 "  }" +
                 "  const text = document.body.innerText; " +
                 "  const phoneMatches = text.match(/(?:Tel|Tél|Phone|Contact|Appeler)\\s*[:-]?\\s*(\\d{8})/i); " +
                 "  return phoneMatches ? phoneMatches[1] : '';" +
                 "}").toString();
            
            if (directPhone != null && !directPhone.isEmpty() && directPhone.matches("\\d{8}")) {
                String cleanedPhone = cleanPhoneNumber(directPhone); // Use cleaner
                if (cleanedPhone != null) {
                    return cleanedPhone;
                }
            }

            // --- Step 2: Check the description text --- 
            if (property.getDescription() != null && !property.getDescription().isEmpty()) {
                String phoneFromDesc = findAndCleanPhoneNumber(property.getDescription());
                if (phoneFromDesc != null) {
                    return phoneFromDesc;
                }
            }

            // --- Step 3: Try the 'Afficher le numéro' modal interaction --- 
            ElementHandle phoneButton = null;
            String[] phoneButtonSelectors = {
                "a.show-tel-side",
                "a[data-target='#phoneModal']",
                "a.show-tel",
                "a:has-text('Afficher le numéro')",
                 // Add other potential button selectors if needed
                ".btn-phone", "button:contains('Phone')", "button:contains('Numéro')"
            };

            for (String selector : phoneButtonSelectors) {
                try {
                    ElementHandle button = page.querySelector(selector);
                    if (button != null && button.isVisible()) {
                        phoneButton = button;
                        log.info("Found phone button with selector: {}", selector);
                        break;
                    }
                 } catch (Exception e) { log.debug("Selector error for phone button '{}': {}", selector, e.getMessage()); }
            }

            if (phoneButton != null) {
                log.info("Clicking phone button to open modal");
                try {
                    phoneButton.click();
                    page.waitForSelector("#phoneModal", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
                    log.info("Phone modal opened successfully");
                    page.waitForTimeout(1000); // Brief pause for modal content
                    
                    page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get("./phone-modal-opened.png")));
                    
                    ElementHandle phoneInputEl = page.querySelector("#prospectPhone");
                    if (phoneInputEl != null && phoneInputEl.isVisible()) { // Check visibility
                        log.info("Found phone input field, filling with number");
                        phoneInputEl.fill("29867984");
                        phoneInputEl.press("Enter"); // Trigger event to enable button
                        log.info("Filled input and triggered Enter key press.");

                        // Wait for button to be visible and enabled before clicking
                        try {
                            log.info("Waiting for submit button to be visible and enabled...");
                            // Ensure button exists before waiting for state
                            Locator submitButton = page.locator("#phoneModal .modal-footer button:has-text('Accéder au numéro')");
                            submitButton.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(2000));
                            // Check if button is enabled (not disabled)
                            boolean isEnabled = !submitButton.isDisabled();
                            if (!isEnabled) {
                                log.info("Submit button is visible but disabled, waiting up to 3s for it to become enabled...");
                                // Additional wait to see if button becomes enabled
                                page.waitForTimeout(3000);
                                isEnabled = !submitButton.isDisabled();
                            }

                            if (isEnabled) {
                                log.info("Submit button is enabled, clicking...");
                                submitButton.click();
                                log.info("Clicked 'Accéder au numéro'. Waiting for number reveal...");

                                // Wait for the phone number link to appear in the modal body
                                Locator revealedPhoneLink = page.locator("#phoneModal .modal-body a[href^='tel:']");
                                revealedPhoneLink.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000)); // Wait up to 5s
                                log.info("Revealed phone link appeared in modal body.");

                                // 3. Extract phone number from the link's href
                                String telHref = revealedPhoneLink.getAttribute("href");
                                if (telHref != null && telHref.startsWith("tel:")) {
                                    String revealedPhoneNumber = telHref.substring(4).replaceAll("[^\\d/]", ""); // Extract digits and slash only

                                    // Additional cleanup/validation if needed (using the updated regex)
                                    final Pattern phonePattern = Pattern.compile("(?:\\+216\\s?)?(\\d{8}(?:\\s*\\/\\s*\\d{8})?)");
                                    Matcher phoneMatcherVar = phonePattern.matcher(revealedPhoneNumber);
                                    if (phoneMatcherVar.find()) {
                                        revealedPhoneNumber = phoneMatcherVar.group(1); // Get the core number part
                                        property.setContactPhone(revealedPhoneNumber);
                                        log.info("Successfully extracted phone number from modal interaction: {}", revealedPhoneNumber);
                                    } else {
                                        log.warn("Extracted href '/{} /' did not match expected phone pattern after cleanup.", revealedPhoneNumber);
                                    }
                                } else {
                                    log.warn("Could not extract href or href did not start with 'tel:' from revealed link.");
                                }

                            }
                        } catch (TimeoutError e) {
                            log.warn("Submit button did not become visible/enabled or revealed phone link did not appear within timeout: {}", e.getMessage());
                        }
                    } else {
                        log.info("Phone number not found directly, attempting interaction...");
                        // 2. If not found directly, interact with the phone reveal modal
                        try {
                            ElementHandle showPhoneButton = page.querySelector("a[data-target='#phoneModal']:has-text('Afficher le numéro')");
                            if (showPhoneButton != null) {
                                log.info("Found 'Afficher le numéro' button, clicking...");
                                showPhoneButton.click();

                                // Wait for the modal to appear
                                try {
                                    page.waitForSelector("#phoneModal", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
                                    log.info("Phone modal is visible.");

                                    // Find and fill the input field using its specific ID
                                    Locator phoneInputLocator = page.locator("#prospectPhone");
                                    // Find the submit button in the footer
                                    Locator submitButton = page.locator("#phoneModal .modal-footer button:has-text('Accéder au numéro')");

                                    if (phoneInputLocator.isVisible()) { // Check only for input visibility initially
                                        log.info("Modal input field found. Interacting...");
                                        phoneInputLocator.fill("29867984"); // Using the example number
                                        phoneInputLocator.press("Enter"); // Trigger event to enable button
                                        log.info("Filled input and triggered Enter key press.");

                                        // Wait for button to be visible and enabled before clicking
                                        try {
                                            log.info("Waiting for submit button to be visible and enabled...");
                                            // Ensure button exists before waiting for state
                                            submitButton.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(2000));
                                            // Check if button is enabled (not disabled)
                                            boolean isEnabled = !submitButton.isDisabled();
                                            if (!isEnabled) {
                                                log.info("Submit button is visible but disabled, waiting up to 3s for it to become enabled...");
                                                // Additional wait to see if button becomes enabled
                                                page.waitForTimeout(3000);
                                                isEnabled = !submitButton.isDisabled();
                                            }

                                            if (isEnabled) {
                                                log.info("Submit button is enabled, clicking...");
                                                submitButton.click();
                                                log.info("Clicked 'Accéder au numéro'. Waiting for number reveal...");

                                                // Wait for the phone number link to appear in the modal body
                                                Locator revealedPhoneLink = page.locator("#phoneModal .modal-body a[href^='tel:']");
                                                revealedPhoneLink.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000)); // Wait up to 5s
                                                log.info("Revealed phone link appeared in modal body.");

                                                // 3. Extract phone number from the link's href
                                                String telHref = revealedPhoneLink.getAttribute("href");
                                                if (telHref != null && telHref.startsWith("tel:")) {
                                                    String revealedPhoneNumber = telHref.substring(4).replaceAll("[^\\d/]", ""); // Extract digits and slash only

                                                    // Additional cleanup/validation if needed (using the updated regex)
                                                    final Pattern phonePattern = Pattern.compile("(?:\\+216\\s?)?(\\d{8}(?:\\s*\\/\\s*\\d{8})?)");
                                                    Matcher phoneMatcherVar = phonePattern.matcher(revealedPhoneNumber);
                                                    if (phoneMatcherVar.find()) {
                                                        revealedPhoneNumber = phoneMatcherVar.group(1); // Get the core number part
                                                        property.setContactPhone(revealedPhoneNumber);
                                                        log.info("Successfully extracted phone number from modal interaction: {}", revealedPhoneNumber);
                                                    } else {
                                                        log.warn("Extracted href '/{} /' did not match expected phone pattern after cleanup.", revealedPhoneNumber);
                                        }
                                    } else {
                                                    log.warn("Could not extract href or href did not start with 'tel:' from revealed link.");
                                                }
                                            }
                                        } catch (TimeoutError e) {
                                            log.warn("Submit button did not become visible/enabled or revealed phone link did not appear within timeout: {}", e.getMessage());
                                }
                            } else {
                                        log.warn("Could not find phone input #prospectPhone in the modal.");
                                    }
                                } catch (TimeoutError e) {
                                    log.warn("Phone modal did not appear or timed out.");
                                }

                                // 4. Fallback: If interaction failed to get number, try general extraction again
                                // Ensure we only run fallback if property phone is still null/empty after interaction attempt
                                if (property != null && (property.getContactPhone() == null || property.getContactPhone().isEmpty())) {
                                    log.info("Falling back to general page scan after interaction attempt.");
                                    String fallbackNumber = extractPhoneNumber(page, property); // Use a separate variable
                                    if (fallbackNumber != null && !fallbackNumber.isEmpty()) {
                                        property.setContactPhone(fallbackNumber);
                                        log.info("Successfully extracted phone number via fallback scan: {}", fallbackNumber);
                        } else {
                                        log.warn("Fallback scan also failed to find phone number after interaction.");
                                    }
                        }
                    } else {
                                log.info("'Afficher le numéro' button not found. Cannot attempt interaction.");
                            }
                        } catch (Exception e) {
                            log.warn("Error during phone number modal interaction: {}", e.getMessage());
                        }
                    }
                } catch (PlaywrightException pe) {
                    log.warn("PlaywrightException during modal interaction: {}", pe.getMessage());
                } catch (Exception e) {
                    log.error("Unexpected error during modal interaction: {}", e.getMessage(), e);
                }
            } else {
                log.info("No 'Afficher le numéro' button found or interactable.");
            }
            
            // --- Step 4: Fallback to searching the entire page text --- 
            if (property.getContactPhone() == null) {
                log.info("Phone not found in description or modal, falling back to page text search.");
                String pageText = page.evaluate("() => document.body.innerText").toString();
                String phoneFromPageText = findAndCleanPhoneNumber(pageText);
                
                if (phoneFromPageText != null) {
                    property.setContactPhone(phoneFromPageText);
                    log.info("Found and cleaned phone number in page text fallback: {}", phoneFromPageText);
                }
            }

            // --- Final Step: Log if no number found anywhere --- 
            if (property.getContactPhone() == null) {
                 log.warn("Could not extract any phone number from the page for URL: {}", page.url());
            }
            
        } catch (Exception e) {
            log.error("Error during phone number extraction process for URL {}: {}", page.url(), e.getMessage(), e);
        }
        return property.getContactPhone(); // Return the phone number stored in property, or null if none was found
    }

    /**
     * Cleans a string potentially containing a phone number by removing common separators 
     * and the international prefix, then extracts the first valid 8-digit number.
     *
     * @param phoneString The raw string.
     * @return An 8-digit phone number string or null if not found.
     */
    private String cleanPhoneNumber(String phoneString) {
        if (phoneString == null || phoneString.trim().isEmpty()) {
            return null;
        }
        
        log.debug("Cleaning phone string: '{}'", phoneString);
        
        // Remove common separators like spaces, hyphens, dots, slashes, parentheses and the +216 prefix
        // Escape the hyphen (-) inside the character class
        String cleaned = phoneString.replaceAll("[\\s\\-/\\.\\(\\)]+", "").replaceFirst("^\\+?216", "");
        
        log.debug("After basic cleaning: '{}'", cleaned);
        
        // Find the first 8-digit sequence
        Pattern pattern = Pattern.compile("(\\d{8})");
        Matcher matcher = pattern.matcher(cleaned);
        
        if (matcher.find()) {
            String result = matcher.group(1);
            log.debug("Found phone number: '{}'", result);
            return result;
        } else {
            // Special check for formats like 'xx xxx xxx' that might have been missed if spaces were crucial before
             if (phoneString.matches(".*\\d{2}\\s+\\d{3}\\s+\\d{3}.*")) {
                 // Fixed incorrect Java string escaping with single backslash
                 String digitsOnly = phoneString.replaceAll("[^\\d]", "");
                 log.debug("Special format detected, digits only: '{}'", digitsOnly);
                 if (digitsOnly.length() >= 8) {
                    // Attempt to find 8 digits within this pattern
                    Matcher spacedMatcher = pattern.matcher(digitsOnly);
                     if (spacedMatcher.find()) {
                         String result = spacedMatcher.group(1);
                         log.debug("Found phone from special format: '{}'", result);
                         return result;
                     }
                 }
             }
            log.debug("No valid phone number found in '{}'", phoneString);
            return null; // No 8-digit number found
        }
    }
    
    /**
     * Searches a larger block of text for multiple potential phone numbers 
     * (handling formats like 'num1 / num2'), cleans them, and returns the first valid one.
     *
     * @param text The text block (e.g., description, modal body, page body).
     * @return The first valid 8-digit phone number found, or null.
     */
    private String findAndCleanPhoneNumber(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        log.debug("Searching for phone number in text: '{}'", text.length() > 50 ? text.substring(0, 50) + "..." : text);

        // First try direct international format as shown in the modal
        Pattern internationalPattern = Pattern.compile("\\+216\\s*\\d{2}\\s*\\d{2}\\s*\\d{2}\\s*\\d{2}");
        Matcher internationalMatcher = internationalPattern.matcher(text);
        if (internationalMatcher.find()) {
            String internationalNumber = internationalMatcher.group(0);
            log.debug("Found international format: '{}'", internationalNumber);
            String cleaned = cleanPhoneNumber(internationalNumber);
            if (cleaned != null) {
                return cleaned;
            }
        }

        // Handle multiple numbers separated by slash (common pattern observed)
        if (text.contains("/")) {
            log.debug("Multiple phone numbers detected (separated by /)");
            String[] potentialNumbers = text.split("/");
            
            for (String potential : potentialNumbers) {
                log.debug("Checking potential phone number: '{}'", potential.trim());
                String cleaned = cleanPhoneNumber(potential);
                if (cleaned != null) {
                    log.debug("Found valid phone from split: '{}'", cleaned);
                    return cleaned; // Return the first valid one found
                }
            }
        }
        
        // If split by '/' didn't work, try cleaning the whole text block
        // This might catch numbers not separated by slash but having other issues
        String cleanedFullText = cleanPhoneNumber(text);
        if (cleanedFullText != null) {
            log.debug("Found phone number from full text: '{}'", cleanedFullText);
            return cleanedFullText;
        }

        // Try to find a number with a specific format (xx xxx xxx) common in Tunisia
        Pattern tunisianFormat = Pattern.compile("\\d{2}\\s+\\d{3}\\s+\\d{3}");
        Matcher tunisianMatcher = tunisianFormat.matcher(text);
        if (tunisianMatcher.find()) {
            String tunisianNumber = tunisianMatcher.group(0);
            log.debug("Found Tunisian format: '{}'", tunisianNumber);
            String cleaned = cleanPhoneNumber(tunisianNumber);
            if (cleaned != null) {
                return cleaned;
            }
        }

        log.debug("No phone number found in text");
        return null; // No valid number found in the text block
    }
    
    /**
     * Extract room count from title or description text
     */
    private void extractRoomsFromText(ImmobilierProperty property, String text) {
        if (text == null || text.isEmpty() || property.getRooms() != null) { // Skip if already found
            return;
        }
        
        log.debug("Attempting to extract room count from text: {}", text.substring(0, Math.min(text.length(), 50)) + "...");

        // --- Adjusted S+N / SN Pattern Logic ---
        // Priority 1: Look for S+N or SN (e.g., S+3, S3, s+2, s2)
        // S+N means N Bedrooms, so N+1 total Rooms.
        Pattern sPattern = Pattern.compile("\\bS\\s*\\+?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher sMatcher = sPattern.matcher(text);
        if (sMatcher.find()) {
            try {
                // N bedrooms found, total rooms = N + 1
                int n = Integer.parseInt(sMatcher.group(1));
                int rooms = n + 1;
                property.setRooms(rooms);
                log.info("Extracted room count ({}) from text (S+?N pattern interpreted as N+1 rooms)", rooms);
                // We could potentially set bedrooms directly here: property.setBedrooms(n);
                // But calculation is handled in extractData for consistency.
                return;
            } catch (NumberFormatException e) {
                log.debug("Could not parse room count from S+?N pattern in text");
            }
        }
        // --- End Adjusted Logic ---
        
        // Pattern 2: N chambres/pièces (Assume this means N total rooms)
        Pattern roomPattern = Pattern.compile("(\\d+)\\s*(?:chambres?|pieces?|pièces?|rooms?|bedrooms?)", Pattern.CASE_INSENSITIVE);
        Matcher roomMatcher = roomPattern.matcher(text);
        if (roomMatcher.find()) {
            try {
                int rooms = Integer.parseInt(roomMatcher.group(1));
                property.setRooms(rooms);
                log.info("Extracted room count ({}) from text ('N chambres' pattern)", rooms);
                return;
            } catch (NumberFormatException e) {
                log.debug("Could not parse room count from chambres pattern in text");
            }
        }
        
        // Pattern 3: Standalone numbers + room indicators (Assume N total rooms)
        Pattern numberPattern = Pattern.compile("\\b(\\d+)\\s*(?:ch|room|chambres?|pieces?|pièces?)\\b", Pattern.CASE_INSENSITIVE);
        Matcher numberMatcher = numberPattern.matcher(text);
        if (numberMatcher.find()) {
            try {
                int rooms = Integer.parseInt(numberMatcher.group(1));
                property.setRooms(rooms);
                log.info("Extracted room count ({}) from text (abbreviated pattern)", rooms);
                return;
            } catch (NumberFormatException e) {
                log.debug("Could not parse room count from abbreviated pattern in text");
            }
        }
        
        // Pattern 4: Try to find T1, T2... (Assume T(N) means N total rooms)
        Pattern tPattern = Pattern.compile("\\bT(\\d+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher tMatcher = tPattern.matcher(text);
        if (tMatcher.find()) {
            try {
                int rooms = Integer.parseInt(tMatcher.group(1));
                property.setRooms(rooms);
                log.info("Extracted room count ({}) from text (T pattern)", rooms);
                return;
            } catch (NumberFormatException e) {
                log.debug("Could not parse room count from T pattern in text");
            }
        }
        
        // Pattern 5: Try to find F1, F2... (Assume F(N) means N total rooms)
        Pattern fPattern = Pattern.compile("\\bF(\\d+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher fMatcher = fPattern.matcher(text);
        if (fMatcher.find()) {
            try {
                int rooms = Integer.parseInt(fMatcher.group(1));
                property.setRooms(rooms);
                log.info("Extracted room count ({}) from text (F pattern)", rooms);
                return;
            } catch (NumberFormatException e) {
                log.debug("Could not parse room count from F pattern in text");
            }
        }
        
        // Pattern 6: Try to find numbers followed by "P" (Assume N total rooms)
        Pattern pPattern = Pattern.compile("\\b(\\d+)\\s*P\\b", Pattern.CASE_INSENSITIVE);
        Matcher pMatcher = pPattern.matcher(text);
        if (pMatcher.find()) {
            try {
                int rooms = Integer.parseInt(pMatcher.group(1));
                property.setRooms(rooms);
                log.info("Extracted room count ({}) from text (P pattern)", rooms);
                return;
            } catch (NumberFormatException e) {
                log.debug("Could not parse room count from P pattern in text");
            }
        }
        
        // Pattern 7: Fallback for studio
            if (text.toLowerCase().contains("studio")) {
            property.setRooms(1); // Studio is 1 room total
                log.info("Setting room count to 1 for studio based on text");
                return;
            }
            
        // Remove the incorrect fallback for 's+N' hints here, as it's handled by Pattern 1 now.

        log.debug("Could not extract room count from text using any pattern.");
    }
    
    /**
     * Geocode the property address
     */
    private void geocodeProperty(ImmobilierProperty property) {
        // --- Check if coordinates were already extracted from the page script ---
        if (property.getLatitude() != null && property.getLongitude() != null) {
            log.info("Using coordinates already extracted from page script: {}, {}", 
                property.getLatitude(), property.getLongitude());
            // Ensure formattedAddress reflects this if it was set during extraction
            if (property.getFormattedAddress() == null || !property.getFormattedAddress().equals("Coordinates from page script")) {
                 property.setFormattedAddress("Coordinates from page script");
            }
            return; // Skip API call and fallback
        }
        // --- End check ---
        
        log.info("Coordinates not found in script, proceeding with API/Fallback geocoding.");

        // Build a cleaner address specifically for the API
        String addressForApi = buildCleanAddressForApi(property);
        if (addressForApi == null || addressForApi.length() < 5) {
            log.warn("Could not build sufficient address for API from City={}, District={}, Location={}. Attempting fallback.", 
                property.getCity(), property.getDistrict(), property.getLocation());
            setFallbackCoordinates(property); // Attempt fallback directly
            return;
        }
        
        double[] coordinates = null; 
        boolean serviceCalled = false;

        // Try using the GeocodingService bean if available
        if (applicationContext != null) {
            try {
                com.example.scraper.service.GeocodingService geocodingService = 
                    applicationContext.getBean(com.example.scraper.service.GeocodingService.class);
                
                if (geocodingService != null) {
                    log.info("Attempting geocoding with service for address: {}", addressForApi);
                    serviceCalled = true;
                    coordinates = geocodingService.geocode(addressForApi);
                    
                    if (coordinates != null && coordinates.length == 2) {
                        log.info("Geocoding service SUCCEEDED for address: {}", addressForApi);
                    } else {
                        log.warn("Geocoding service returned null/invalid coordinates for address: {}. Will use fallback.", addressForApi);
                        coordinates = null; // Ensure coordinates are null if service failed
                    }
                }
            } catch (Exception e) {
                log.warn("Error using GeocodingService bean for address {}: {}. Proceeding to fallback.", addressForApi, e.getMessage());
                coordinates = null; // Ensure coordinates are null if service errored
            }
        } else {
             log.warn("ApplicationContext not available, cannot use GeocodingService. Proceeding to fallback.");
        }

        // If service wasn't called or failed, use fallback
        if (coordinates == null) {
            if (serviceCalled) {
                 log.info("Geocoding service failed or returned null, using fallback logic.");
            }
            setFallbackCoordinates(property); 
        } else {
            // Service succeeded, set the coordinates
             property.setLatitude(coordinates[0]);
             property.setLongitude(coordinates[1]);
             // Use the original fullAddress for display/storage if available
             String displayAddress = property.getFullAddress() != null && !property.getFullAddress().trim().isEmpty() 
                                    ? property.getFullAddress() 
                                    : addressForApi; // Fallback display address
             property.setFormattedAddress(displayAddress); 
             log.info("Set coordinates via geocoding service: {}, {} for address: {}", coordinates[0], coordinates[1], addressForApi);
        }
    }

    /**
     * Builds a cleaner address string optimized for the Geocoding API.
     * Prioritizes specific district/city information.
     */
    private String buildCleanAddressForApi(ImmobilierProperty property) {
        StringBuilder sb = new StringBuilder();
        String district = property.getDistrict();
        String city = property.getCity();
        String location = property.getLocation(); // Original location string from title parsing

        // Clean inputs
        district = (district != null) ? district.trim() : "";
        city = (city != null) ? city.trim() : "";
        location = (location != null) ? location.trim() : "";

        // 1. Use District if specific and not identical to city
        if (!district.isEmpty() && !district.equalsIgnoreCase(city)) {
            sb.append(district);
        }

        // 2. Add City if specific and different from district (or if district was empty)
        if (!city.isEmpty() && !city.equalsIgnoreCase(district)) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(city);
        }
        
        // 3. If we only have identical city/district, just use one
        if (sb.length() == 0 && !city.isEmpty() && city.equalsIgnoreCase(district)) {
            sb.append(city); 
        }

        // 4. If district/city provided nothing useful, try parsing the raw location string 
        // (This often contains district, city, etc. but might be messy)
        if (sb.length() == 0 && !location.isEmpty()) {
            // Take the first part of the location string before a comma, assuming it's the most specific part
            String[] parts = location.split(",");
             if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                 sb.append(parts[0].trim()); 
                 // Optionally add the second part if it looks like a distinct city/region
                 if (parts.length > 1 && !parts[1].trim().isEmpty() && !parts[1].trim().equalsIgnoreCase(parts[0].trim())) {
                     sb.append(", ").append(parts[1].trim());
                 }
             } else {
                sb.append(location); // Use raw location if no comma
             }
        }
        
        // If still empty, cannot proceed
        if (sb.length() == 0) {
             log.warn("Could not build a meaningful address string for geocoding API from: City='{}', District='{}', Location='{}'", city, district, location);
             return null;
        }

        // Always append Tunisia for context
        sb.append(", Tunisia");
        String finalAddress = sb.toString().replaceAll("[\n\r]+", " ").trim(); // Remove newlines and trim
        log.debug("Built address for API: {}", finalAddress);
        return finalAddress;
    }

    /**
     * Sets fallback coordinates based on district or city if primary geocoding fails.
     */
    private void setFallbackCoordinates(ImmobilierProperty property) {
        log.warn("Setting fallback coordinates for property ID: {}, original address: {}", property.getId(), property.getFullAddress());
        String district = property.getDistrict() != null ? property.getDistrict().toLowerCase().trim() : "";
        String city = property.getCity() != null ? property.getCity().toLowerCase().trim() : "";
            
        Map<String, double[]> fallbackMap = getFallbackCoordinateMap(); 

        double[] coords = null;
        String usedKey = null;

        List<String> sortedKeys = fallbackMap.keySet().stream()
            .sorted((a, b) -> Integer.compare(b.length(), a.length())) // Match longer keys first (e.g., 'borj louzir' before 'lac')
            .collect(Collectors.toList());

        // Check district first using sorted keys
        if (!district.isEmpty()) {
            for (String key : sortedKeys) {
                if (district.contains(key)) {
                    coords = fallbackMap.get(key);
                    usedKey = key + " (district)";
                    break;
                }
            }
        }

        // If not found by district, check city using sorted keys
        if (coords == null && !city.isEmpty()) {
             for (String key : sortedKeys) {
                if (city.contains(key)) {
                    coords = fallbackMap.get(key);
                    usedKey = key + " (city)";
                    break;
                }
            }
        }

        // If still not found, default to Tunis
        if (coords == null) {
            coords = fallbackMap.get("tunis");
            usedKey = "tunis (default)";
            log.warn("No specific fallback match found for District: '{}', City: '{}'. Defaulting to Tunis.", district, city);
        }

        if (coords != null) {
            property.setLatitude(coords[0]);
            property.setLongitude(coords[1]);
            property.setFormattedAddress("Fallback based on: " + usedKey);
            log.info("Set fallback coordinates via map: {}, {} based on key: {}", coords[0], coords[1], usedKey);
        } else {
            // This should ideally never happen if "tunis" is in the map
            log.error("CRITICAL: Could not determine fallback coordinates, default Tunis key missing from map?");
        }
    }

    // Helper method to access fallback coordinates (could be static or injected)
    private Map<String, double[]> getFallbackCoordinateMap() {
        // This ideally shouldn't be duplicated, but needed for separation for now
        Map<String, double[]> map = new HashMap<>();
        map.put("tunis", new double[]{36.8065, 10.1815});
        map.put("sfax", new double[]{34.7406, 10.7603});
        map.put("sousse", new double[]{35.8245, 10.6346});
        map.put("kairouan", new double[]{35.6781, 10.1014});
        map.put("bizerte", new double[]{37.2746, 9.8748});
        map.put("gabes", new double[]{33.8869, 10.0982});
        map.put("ariana", new double[]{36.8625, 10.1956});
        map.put("gafsa", new double[]{34.4311, 8.7757});
        map.put("monastir", new double[]{35.7643, 10.8113});
        map.put("ben arous", new double[]{36.7533, 10.2282});
        map.put("kasserine", new double[]{35.1667, 8.8333});
        map.put("medenine", new double[]{33.3547, 10.5053});
        map.put("nabeul", new double[]{36.4513, 10.7357});
        map.put("tataouine", new double[]{32.9297, 10.4518});
        map.put("beja", new double[]{36.7333, 9.1833});
        map.put("jendouba", new double[]{36.5011, 8.7803});
        map.put("el kef", new double[]{36.1672, 8.7047});
        map.put("mahdia", new double[]{35.5047, 11.0622});
        map.put("sidi bouzid", new double[]{35.0381, 9.4858});
        map.put("tozeur", new double[]{33.9197, 8.1335});
        map.put("siliana", new double[]{36.0878, 9.3733});
        map.put("zaghouan", new double[]{36.4019, 10.1422});
        map.put("kebili", new double[]{33.7046, 8.9646});
        map.put("lac 1", new double[]{36.8317, 10.2475}); // More specific Lac 1
        map.put("lac 2", new double[]{36.8440, 10.2685}); // More specific Lac 2
        map.put("berges du lac 1", new double[]{36.8317, 10.2475});
        map.put("berges du lac 2", new double[]{36.8440, 10.2685});
        map.put("lac", new double[]{36.8317, 10.2475}); // General Lac
        map.put("marsa", new double[]{36.8842, 10.3230});
        map.put("sidi bou said", new double[]{36.8702, 10.3413});
        map.put("carthage", new double[]{36.8589, 10.3336});
        map.put("gammarth", new double[]{36.9181, 10.2903});
        map.put("menzah 1", new double[]{36.840, 10.175}); // Specific Menzah areas
        map.put("menzah 4", new double[]{36.845, 10.165});
        map.put("menzah 5", new double[]{36.842, 10.158});
        map.put("menzah 6", new double[]{36.848, 10.155});
        map.put("menzah 7", new double[]{36.853, 10.160});
        map.put("menzah 8", new double[]{36.858, 10.165});
        map.put("menzah 9", new double[]{36.8451, 10.1686});
        map.put("menzah", new double[]{36.8345, 10.1686}); // General Menzah
        map.put("jardins d'el menzah 1", new double[]{36.850, 10.175});
        map.put("jardins d'el menzah 2", new double[]{36.855, 10.180});
        map.put("bardo", new double[]{36.8088, 10.1400});
        map.put("lafayette", new double[]{36.8016, 10.1853});
        map.put("mornag", new double[]{36.6697, 10.2897});
        map.put("la soukra", new double[]{36.8982, 10.2378});
        map.put("borj louzir", new double[]{36.8982, 10.2378}); // Same as La Soukra, common area
        map.put("el manar 1", new double[]{36.8393, 10.1584});
        map.put("el manar 2", new double[]{36.842, 10.150});
        map.put("el manar", new double[]{36.8393, 10.1584}); // General El Manar
        map.put("ennasr 1", new double[]{36.860, 10.145});
        map.put("ennasr 2", new double[]{36.865, 10.140});
        map.put("ennasr", new double[]{36.862, 10.142}); // General Ennasr
        map.put("mutuelleville", new double[]{36.825, 10.178});
        map.put("el aouina", new double[]{36.876, 10.255});
        return map;
    }

    // Helper method to resolve relative image URLs
    private String resolveImageUrl(String relativeUrl, String baseUrl) {
        if (relativeUrl == null || relativeUrl.trim().isEmpty()) {
            return null;
        }
        if (relativeUrl.startsWith("http") || relativeUrl.startsWith("//")) {
            return relativeUrl.startsWith("//") ? "https:" + relativeUrl : relativeUrl;
        }
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL resolved = new java.net.URL(base, relativeUrl);
            return resolved.toString();
        } catch (java.net.MalformedURLException e) {
            log.warn("Could not create absolute image URL from base {} and relative {}: {}", baseUrl, relativeUrl, e.getMessage());
            return null; // Return null if resolution fails
        }
    }

    @Override
    public String getScraperName() {
        return "PlaywrightImmobilierScraper";
    }
} 