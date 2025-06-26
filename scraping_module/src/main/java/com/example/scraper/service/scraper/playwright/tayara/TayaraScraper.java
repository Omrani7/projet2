package com.example.scraper.service.scraper.playwright.tayara;

import com.example.scraper.model.TayaraProperty;
import com.example.scraper.service.scraper.playwright.AbstractPlaywrightScraper;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
// Import GeocodingService if you have it in this module, otherwise it might be in spring-security
import com.example.scraper.service.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.TimeoutError;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.web.util.UriComponentsBuilder;

@Service("playwrightTayaraScraper")
@Slf4j
public class TayaraScraper extends AbstractPlaywrightScraper<TayaraProperty> {

    private static final String BASE_URL = "https://www.tayara.tn";
    private static final int MAX_PAGES_TO_SCRAPE = 2;
    private static final long DELAY_BETWEEN_PAGES_MS = 2000; // 2 seconds
    private static final int MAX_ANNOUNCEMENT_AGE_DAYS = 30; // Maximum age for an announcement to be considered recent

    // Selector for the "Next Page" button - user provided, may need refinement for robustness
    private static final String NEXT_PAGE_BUTTON_SELECTOR = "#__next > div.flex.flex-col.xl\\:flex-row.h-fit.w-full.overflow-x-hidden.mt-\\[13rem\\].lg\\:mt-\\[9rem\\].max-w-\\[1920px\\].lg\\:px-12.mx-auto > main > div.mt-3.mx-2.lg\\:ml-0.lg\\:mt-12.relative.z-10 > div.relative.-z-40 > div:nth-child(3) > div > div > button.flex.items-center.justify-center.px-3.h-8.text-sm.font-light.text-neutral-700.border-0.rounded-md";

    @Autowired
    private GeocodingService geocodingService;

    @Override
    public List<TayaraProperty> scrape(String initialUrl) {
        log.info("[TayaraScraper] Received initialUrl: '{}'", initialUrl); // Log 1: initialUrl at start

        String baseUrlToUse = BASE_URL + "/ads/c/Immobilier"; // Default
        boolean useCustomUrl = false;

        if (initialUrl != null && !initialUrl.isEmpty()) {
            try {
                URI uri = new URI(initialUrl); // Should be decoded if coming from Spring @RequestParam
                if ("www.tayara.tn".equalsIgnoreCase(uri.getHost()) && uri.getPath() != null && uri.getPath().startsWith("/ads/")) {
                    // Further check: ensure the scheme is http or https
                    if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                        baseUrlToUse = initialUrl; // Use the decoded URL as is
                        useCustomUrl = true;
                        log.info("[TayaraScraper] Using custom valid URL: '{}'", baseUrlToUse);
                    } else {
                        log.warn("[TayaraScraper] Provided URL '{}' has invalid scheme '{}'. Defaulting.", initialUrl, uri.getScheme());
                    }
                } else {
                    log.warn("[TayaraScraper] Provided URL '{}' did not meet host/path criteria. Host: '{}', Path: '{}'. Defaulting.", initialUrl, uri.getHost(), uri.getPath());
                }
            } catch (URISyntaxException e) {
                log.warn("[TayaraScraper] Provided URL '{}' was malformed. Defaulting. Error: {}", initialUrl, e.getMessage());
            }
        } else {
            log.info("[TayaraScraper] initialUrl is null or empty. Defaulting.");
        }

        if (!useCustomUrl) {
             log.warn("[TayaraScraper] Defaulting to base Immobilier URL: '{}'", baseUrlToUse);
        }

        List<TayaraProperty> allProperties = new ArrayList<>();
        int currentPage = 1;
        boolean hasNextPage = true;
        int pagesProcessedCount = 0; // Counter for pages where extraction was attempted

        // Ensure Playwright context is managed correctly (from AbstractPlaywrightScraper)
        // This typically involves: Playwright playwright = Playwright.create(); Browser browser = getBrowser(playwright); BrowserContext context = browser.newContext(...);
        // For simplicity, assuming getPlaywrightPage() in AbstractPlaywrightScraper handles this setup or we adapt it.
        // The actual page navigation and data extraction will happen within the loop using a Page object.

        try (Playwright playwright = Playwright.create()) {
            Browser browser = getBrowser(playwright); // Assuming getBrowser() is accessible or defined
            BrowserContext browserContext = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT));
            
            while (currentPage <= MAX_PAGES_TO_SCRAPE && hasNextPage) {
                Page playwrightPage = browserContext.newPage(); // New page for each iteration
                try {
                    String urlForCurrentPage = buildUrlForPage(baseUrlToUse, currentPage);
                    log.info("[TayaraScraper] Attempting to navigate to (Playwright will encode this): '{}'", urlForCurrentPage); // Log 2: Before navigate

                    playwrightPage.navigate(urlForCurrentPage, new Page.NavigateOptions().setTimeout(60000));
                    playwrightPage.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(60000));
                    log.info("[TayaraScraper] Actual URL after navigation and load: '{}'", playwrightPage.url()); // Log 3: After navigate
                    
                    pagesProcessedCount++; // Increment for each page attempt

                    // No need to call super.scrape() or super.performScrapingOnPage() here, 
                    // as that would create new playwright/browser instances repeatedly.
                    // Instead, we directly call extractData with the current page.
                    List<TayaraProperty> propertiesFromPage = extractData(playwrightPage);

                    if (propertiesFromPage.isEmpty() && currentPage > 1) {
                        log.info("No properties found on page {}. Assuming it's the end of results.", currentPage);
                        break; // Stop if a page (other than the first) returns no results
                    }
                    allProperties.addAll(propertiesFromPage);
                    log.info("Found {} properties on page {}. Total properties so far: {}", propertiesFromPage.size(), currentPage, allProperties.size());

                    // Check for next page AFTER processing current page's data
                    ElementHandle nextPageButton = playwrightPage.querySelector(NEXT_PAGE_BUTTON_SELECTOR);
                    if (nextPageButton != null && nextPageButton.isVisible() && nextPageButton.isEnabled()) {
                        log.info("Next page button found and is active on page {}. Proceeding to next page.", currentPage);
                        hasNextPage = true;
                    } else {
                        log.info("Next page button not found or not active on page {}. Assuming end of results.", currentPage);
                        hasNextPage = false;
                    }

                    if (hasNextPage && currentPage < MAX_PAGES_TO_SCRAPE) {
                        try {
                            log.debug("Waiting for {}ms before scraping next page...", DELAY_BETWEEN_PAGES_MS);
                            Thread.sleep(DELAY_BETWEEN_PAGES_MS);
                        } catch (InterruptedException e) {
                            log.warn("Delay between page scrapes was interrupted.");
                            Thread.currentThread().interrupt(); // Restore interruption status
                        }
                    } else if (!hasNextPage) {
                        log.info("Stopping pagination as no active next page button was found.");
                    } else {
                        log.info("Reached MAX_PAGES_TO_SCRAPE limit ({}).", MAX_PAGES_TO_SCRAPE);
                    }

                } catch (TimeoutError e) {
                    log.warn("Timeout navigating to or loading page {}: {}. Assuming no more pages or issue with URL.", currentPage, e.getMessage());
                    hasNextPage = false; // Stop if a page times out
                } catch (Exception e) {
                    log.error("An error occurred while processing page {}: {}", currentPage, e.getMessage(), e);
                    hasNextPage = false; // Stop on other critical errors for a page
                } finally {
                    if (playwrightPage != null && !playwrightPage.isClosed()) {
                        playwrightPage.close();
                    }
                }
                if (hasNextPage) {
                    currentPage++;
                }
            }
            // Closing browserContext and browser should happen after the loop if they are managed here.
            // However, AbstractPlaywrightScraper might manage this at a higher level if we were calling its main scrape method.
            // For this overridden scrape method, we manage the Playwright lifecycle within it.
            if (browserContext != null) browserContext.close();
            // `playwright` will be closed by the try-with-resources statement.
            // `browser` is closed when `playwright` is closed if it was launched by this playwright instance.

        } catch (Exception e) {
            log.error("An error occurred during the multi-page scraping process: {}", e.getMessage(), e);
            // Depending on requirements, might re-throw or return partial results
        }

        log.info("Pagination scraping finished. Total properties extracted from {} pages: {}", pagesProcessedCount, allProperties.size());
        return allProperties;
    }

    private String buildUrlForPage(String baseUrl, int pageNum) {
        // Validate and clean the base URL to prevent common errors
        String cleanedBaseUrl = baseUrl.trim();
        if (cleanedBaseUrl.endsWith("&") || cleanedBaseUrl.endsWith("?")) {
            cleanedBaseUrl = cleanedBaseUrl.substring(0, cleanedBaseUrl.length() - 1);
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(cleanedBaseUrl);
        builder.replaceQueryParam("page", String.valueOf(pageNum));
        String builtUrl = builder.build(true).toUriString(); // build(true) to encode the URI

        log.debug("[TayaraScraper] Built URL for page {}: {}", pageNum, builtUrl);
        return builtUrl;
    }

    @Override
    protected List<TayaraProperty> extractData(Page page) {
        log.info("Navigated to search results page: {}", page.url());
        List<TayaraProperty> properties = new ArrayList<>();

        // **IMPORTANT**: Update this selector to accurately target the age string element on Tayara.tn
        // This selector is based on the structure <div class="flex items-center space-x-2 mb-1"><svg.../><span class="">TEXT</span></div>
        // It targets the span directly. If this structure is not unique, a more specific parent is needed.
        final String AGE_STRING_SELECTOR_PLACEHOLDER = "div.flex.items-center.space-x-2.mb-1 > span"; // Updated based on user input

        String announcementLinkSelector = "div.relative.-z-40 div > div > article > a";
        log.info("Attempting to find listing links with selector: {}", announcementLinkSelector);

        List<ElementHandle> listingLinks;
        try {
            // Wait for the selector to ensure links are loaded
            page.waitForSelector(announcementLinkSelector, new Page.WaitForSelectorOptions().setTimeout(15000)); // 15s timeout
            listingLinks = page.querySelectorAll(announcementLinkSelector);
            log.info("Found {} potential listing links on the page using selector: {}", listingLinks.size(), announcementLinkSelector);

            if (listingLinks.isEmpty()) {
                log.warn("No listing links found with selector '{}' even after waiting. Page: {}", announcementLinkSelector, page.url());
                // No need to try fallback here if waitForSelector succeeded but found nothing immediately.
                // The main check for empty list will handle termination in the scrape() method.
            }

        } catch (com.microsoft.playwright.TimeoutError e) {
            log.warn("Initial timeout waiting for listing links with selector '{}' on page {}. Attempting reload. Error: {}",
                     announcementLinkSelector, page.url(), e.getMessage());
            try {
                log.info("Reloading page: {}", page.url());
                page.reload(new Page.ReloadOptions().setTimeout(60000)); // Added timeout for reload
                page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(60000));
                log.info("Page reloaded. Retrying waitForSelector for: {}", announcementLinkSelector);
                page.waitForSelector(announcementLinkSelector, new Page.WaitForSelectorOptions().setTimeout(15000)); // Retry after reload
                listingLinks = page.querySelectorAll(announcementLinkSelector);
                log.info("Found {} potential listing links after reload on page {}: {}", listingLinks.size(), page.url(), listingLinks.size());
                 if (listingLinks.isEmpty()) {
                    log.warn("No listing links found with selector '{}' after reload. Page: {}", announcementLinkSelector, page.url());
                }
            } catch (com.microsoft.playwright.TimeoutError e2) {
                log.warn("Timeout persisted for selector '{}' even after reloading page {}. Assuming page is empty or has different structure. Error: {}",
                         announcementLinkSelector, page.url(), e2.getMessage());
                return properties; // Return empty list
            } catch (Exception reloadException) {
                log.error("Error during page reload or subsequent selector search on page {}: {}", page.url(), reloadException.getMessage(), reloadException);
                return properties; // Return empty list
            }
        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to find listing links on page {}: {}", page.url(), e.getMessage(), e);
            return properties; // Return empty list on other unexpected errors as well
        }
        
        // The fallback logic for a different selector is removed as the primary issue is timeout/absence on later pages.
        // If selector validity across all pages becomes an issue, that can be revisited.

        int count = 0;
        List<String> processedUrls = new ArrayList<>(); // To avoid processing the same ad twice if it appears on multiple pages (less likely with distinct pages)

        for (ElementHandle cardLink : listingLinks) {
            String detailPagePath = cardLink.getAttribute("href");
            if (detailPagePath == null || detailPagePath.trim().isEmpty()) {
                log.warn("Found a card link with no href attribute. Skipping.");
                continue;
            }

            String detailPageUrl;
            if (detailPagePath.startsWith("http")) {
                detailPageUrl = detailPagePath;
            } else {
                detailPageUrl = BASE_URL + (detailPagePath.startsWith("/") ? "" : "/") + detailPagePath;
            }

            // Ensure it's a valid item URL
            if (!detailPageUrl.contains("/item/")) {
                log.debug("Skipping non-item URL found (possibly with fallback selector): {}", detailPageUrl);
                continue;
            }

            if (processedUrls.contains(detailPageUrl)) {
                log.info("Skipping already processed URL: {}", detailPageUrl);
                continue;
            }
            processedUrls.add(detailPageUrl);

            log.info("Processing listing link ({} / {}): {}", (count + 1), listingLinks.size(), detailPageUrl);

            TayaraProperty property = new TayaraProperty();
            property.setUrl(detailPageUrl);
            property.setSourceWebsite("tayara.tn");

            // Extract ID from URL
            Pattern idPattern = Pattern.compile("/item/([a-zA-Z0-9]+)/");
            Matcher idMatcher = idPattern.matcher(detailPageUrl);
            if (idMatcher.find()) {
                property.setId(idMatcher.group(1));
                log.debug("Extracted ID: {} for URL: {}", property.getId(), detailPageUrl);
            } else {
                log.warn("Could not extract ID from URL: {}. Using partial URL as fallback ID.", detailPageUrl);
                // Create a more unique fallback ID if needed, e.g., by hashing URL
                property.setId(detailPageUrl.substring(Math.max(0, detailPageUrl.length() - 50))); // Last 50 chars
            }

            Page detailPage = null;
            try {
                detailPage = page.context().newPage();
                log.info("Opening detail page for ID {}: {}", property.getId(), detailPageUrl);
                detailPage.navigate(detailPageUrl);
                detailPage.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(60000)); // Wait for DOM, 60s timeout
                log.info("Detail page DOM loaded for ID {}", property.getId());

                // --- Filter by Age BEFORE extensive processing ---
                String rawAgeString = extractText(detailPage, AGE_STRING_SELECTOR_PLACEHOLDER);
                if (rawAgeString == null || rawAgeString.trim().isEmpty()) {
                    log.warn("Could not extract age string for property ID {}. Selector: '{}'. Assuming it might be too new to have a specific age or selector is incorrect. Will attempt to process.", property.getId(), AGE_STRING_SELECTOR_PLACEHOLDER);
                } else {
                    if (!isRecentEnough(rawAgeString)) {
                        log.info("Skipping old announcement ID {}: {} (Age: {})", property.getId(), detailPageUrl, rawAgeString);
                        if (detailPage != null && !detailPage.isClosed()) {
                            detailPage.close();
                        }
                        continue; // Skip to the next listing
                    }
                    log.info("Announcement ID {} is recent enough (Raw age string: {}). Proceeding with extraction.", property.getId(), rawAgeString);
                }
                // --- End Age Filter ---

                // --- Attempt to Extract JSON Data ---
                ElementHandle nextDataScript = detailPage.querySelector("script#__NEXT_DATA__");
                if (nextDataScript != null) {
                    String jsonDataString = nextDataScript.textContent();
                    if (jsonDataString != null && !jsonDataString.isEmpty()) {
                        log.info("Successfully extracted __NEXT_DATA__ JSON string for ID {}. Length: {}", property.getId(), jsonDataString.length());
                        try {
                            populatePropertyFromJson(property, jsonDataString);
                        } catch (Exception e) {
                            log.error("Failed to parse __NEXT_DATA__ JSON for ID {}: {}", property.getId(), e.getMessage(), e);
                        }
                    } else {
                        log.warn("__NEXT_DATA__ script content is empty for ID {}.", property.getId());
                    }
                } else {
                    log.warn("__NEXT_DATA__ script tag not found for ID {}. Will rely on selector-based extraction.", property.getId());
                }

                // --- Selector-based fallbacks or primary extraction if JSON failed/incomplete ---

                // Fallback for Title if not set by JSON
                if (property.getTitle() == null) {
                    log.info("Title not found via JSON for ID {}. Attempting H1 selector.", property.getId());
                    property.setTitle(extractText(detailPage, "h1.text-gray-700.font-bold")); // Example selector for title
                    log.info("Title from H1 selector for ID {}: {}", property.getId(), property.getTitle());
                }

                // Fallback/Alternative for Bedrooms if not set by JSON's adParams
                if (property.getBedrooms() == null) {
                    log.info("Bedrooms not found via JSON adParams for ID {}. Attempting specific 'Chambres' XPath selector.", property.getId());
                    String chambresValueXPath = "//span[contains(@class, 'flex-col') and ./span[1][normalize-space(text())='Chambres']]/span[2]";
                    try {
                        // Use a short timeout for this specific optional element.
                        String chambresText = detailPage.locator(chambresValueXPath).first().textContent(new com.microsoft.playwright.Locator.TextContentOptions().setTimeout(3000));
                        if (chambresText != null && !chambresText.trim().isEmpty()) {
                            try {
                                int chambresCount = Integer.parseInt(chambresText.trim());
                                property.setBedrooms(chambresCount);
                                // Apply studio logic for rooms if title is available
                                if (property.getTitle() != null && property.getTitle().toLowerCase().contains("studio")) {
                                    property.setRooms(chambresCount); // For studio, rooms = bedrooms
                                    log.info("Extracted 'Chambres' via XPath for ID {} (Studio rule): {} -> Bedrooms={}, Rooms={}", property.getId(), chambresText.trim(), property.getBedrooms(), property.getRooms());
                                } else {
                                    property.setRooms(chambresCount + 1); // Default: rooms = bedrooms + 1
                                    log.info("Extracted 'Chambres' via XPath for ID {} (Default rule): {} -> Bedrooms={}, Rooms={}", property.getId(), chambresText.trim(), property.getBedrooms(), property.getRooms());
                                }
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse 'Chambres' value '{}' from XPath for ID {}. Text: '{}'", chambresText.trim(), property.getId(), chambresText);
                            }
                        } else {
                            log.info("'Chambres' value from XPath was null/empty for ID {}.", property.getId());
                        }
                    } catch (com.microsoft.playwright.TimeoutError e) { // More specific exception for locator timeout
                        log.info("'Chambres' element (XPath) timed out for ID {}: {}", property.getId(), e.getMessage().split("\n")[0]);
                    } catch (com.microsoft.playwright.PlaywrightException e) { // Catch other Playwright issues like element not found
                        log.info("'Chambres' element not found via XPath for ID {} or other Playwright error: {}", property.getId(), e.getMessage().split("\n")[0]);
                    }
                }

                // Final fallback: S+N regex parsing on title/description IF BEDROOMS ARE STILL NOT SET
                if (property.getBedrooms() == null) {
                    if (property.getTitle() != null && !property.getTitle().isEmpty()) {
                        log.info("Bedrooms still not found for ID {}. Attempting S+N regex on title/description.", property.getId());
                        parseRoomsAndBedrooms(property); // This sets bedrooms (N) and rooms (N+1)
                    } else {
                        log.info("Bedrooms not found and title is also missing for ID {}. Cannot run S+N regex parsing.", property.getId());
                    }
                } else {
                    // This log might be too verbose if bedrooms are frequently found by earlier methods. Consider removing or making debug.
                    // log.info("Bedrooms already populated for ID {} (Value: {}). Skipping S+N regex as a fallback.", property.getId(), property.getBedrooms());
                }

                // --- Refine Phone Number if incomplete and found in description ---
                String currentPhone = property.getContactPhone();
                if (currentPhone != null && 
                    (currentPhone.matches("^\\s*\\+216\\s*$") || currentPhone.trim().length() < 9) && // Checks for just "+216" or too short
                    property.getDescription() != null && !property.getDescription().isEmpty()) {
                    
                    log.info("Contact phone '{}' for ID {} appears incomplete. Searching in description...", currentPhone, property.getId());
                    String descriptionText = property.getDescription();
                    
                    // Regex to find Tunisian phone numbers (8 digits, optional prefix, optional spaces)
                    // Updated prefix to allow spaces like (00 216).
                    Pattern phonePattern = Pattern.compile("(?:(?:\\(00\\s*216\\))|\\+216)?[\\s.-]*((?:\\d{2}[\\s.-]*){3}\\d{2}|\\d{8})");
                    Matcher phoneMatcher = phonePattern.matcher(descriptionText);
                    
                    if (phoneMatcher.find()) {
                        String foundNumber = phoneMatcher.group(1).replaceAll("[\\s.-]", ""); // Get the 8 digits and remove spaces/dots/hyphens
                        if (foundNumber.length() == 8) { // Validate it's 8 digits
                            String newPhoneNumber = "+216" + foundNumber;
                            log.info("Found phone number '{}' in description for ID {}. Updating from '{}'.", newPhoneNumber, property.getId(), currentPhone);
                            property.setContactPhone(newPhoneNumber);
                        } else {
                            log.warn("Regex matched a potential phone number fragment '{}' in description for ID {}, but it was not 8 digits after normalization.", foundNumber, property.getId());
                        }
                    } else {
                        log.info("No phone number pattern found in description for ID {} despite initial phone being incomplete.", property.getId());
                    }
                }

                // Geocode the property
                if (property.getFullAddress() != null && !property.getFullAddress().isEmpty()) {
                    if (geocodingService != null) { // Ensure service is available
                        try {
                            log.info("Geocoding address for property ID {}: {}", property.getId(), property.getFullAddress());
                            double[] coordsArray = geocodingService.geocode(property.getFullAddress()); // Changed method call and type
                            if (coordsArray != null && coordsArray.length >= 2) {
                                property.setLatitude(coordsArray[0]); // Assuming index 0 is latitude
                                property.setLongitude(coordsArray[1]); // Assuming index 1 is longitude
                                log.info("Geocoding successful for ID {}: Lat={}, Lon={}", property.getId(), coordsArray[0], coordsArray[1]);
                            } else {
                                log.warn("Geocoding returned null or insufficient data for address: {} (ID: {})", property.getFullAddress(), property.getId());
                            }
                        } catch (Exception geoEx) {
                            log.error("Error during geocoding for property ID {} (Address: {}): {}",
                                      property.getId(), property.getFullAddress(), geoEx.getMessage());
                            // Log less verbose stack trace for geocoding errors unless debug is enabled
                            if (log.isDebugEnabled()) {
                                log.debug("Geocoding exception details:", geoEx);
                            }
                            // Decide if you want to set lat/lon to null or some default, or just log.
                            // Property will be added without lat/lon if an error occurs here.
                        }
                    } else {
                         log.warn("GeocodingService is null. Cannot geocode address for property ID {}.", property.getId());
                    }
                } else {
                    log.warn("Full address is null or empty for property ID {}. Skipping geocoding.", property.getId());
                }

                if (property.getTitle() != null && !property.getTitle().isEmpty()) {
                    log.info("Successfully extracted data (and attempted geocoding) for property ID {}: Title='{}'", property.getId(), property.getTitle());
                    properties.add(property);
                } else {
                    log.warn("Could not extract a title for property ID {}. Skipping this property.", property.getId());
                }
                
                count++;
                // REMOVING THE LIMIT: The following block is now commented out
                /*
                if (count >= 500) { // TEMPORARY LIMIT - INCREASE OR REMOVE FOR FULL SCRAPE
                    log.info("Reached temporary scrape limit of {} properties for testing.", count);
                    break;
                }
                */

            } catch (Exception e) {
                log.error("Error processing detail page for ID {} (URL: {}): {}", property.getId(), detailPageUrl, e.getMessage(), e);
            } finally {
                if (detailPage != null && !detailPage.isClosed()) {
                    detailPage.close();
                    log.info("Closed detail page for ID {}", property.getId());
                }
            }
        }

        if (properties.isEmpty() && !listingLinks.isEmpty()) {
            log.warn("Processed {} links but ended up with 0 properties. Check detail page extraction logic and selectors for page: {}", listingLinks.size(), page.url());
        } else if (properties.isEmpty() && listingLinks.isEmpty()) {
             log.warn("No listing links were found or processed into properties for page: {}. (This might be normal for the last page).", page.url());
        }

        log.info("Finished extracting data from page {}. Total properties extracted from this page: {}", page.url(), properties.size());
        return properties;
    }

    // New helper method to orchestrate population from JSON
    private void populatePropertyFromJson(TayaraProperty property, String jsonDataString) {
        log.info("Attempting to parse __NEXT_DATA__ JSON and populate property for ID: {}", property.getId());
        try {
            // --- STEP 1: Parse the JSON string --- 
            // Ensure Jackson dependency is in pom.xml for these lines to work
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> rootJsonData = objectMapper.readValue(jsonDataString, new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) rootJsonData.getOrDefault("props", Collections.emptyMap());
            @SuppressWarnings("unchecked")
            Map<String, Object> pageProps = (Map<String, Object>) props.getOrDefault("pageProps", Collections.emptyMap());
            @SuppressWarnings("unchecked")
            Map<String, Object> adDetails = (Map<String, Object>) pageProps.getOrDefault("adDetails", Collections.emptyMap());
            @SuppressWarnings("unchecked")
            Map<String, Object> adUserData = (Map<String, Object>) pageProps.getOrDefault("adUserData", Collections.emptyMap());
            @SuppressWarnings("unchecked") 
            List<Map<String, Object>> adParams = (List<Map<String, Object>>) pageProps.getOrDefault("adParams", Collections.emptyList());

            // --- SIMULATION BLOCK REMOVED ---
            // log.warn("JSON PARSING IS SIMULATED for ID {}. Uncomment ObjectMapper lines and add Jackson dependency.", property.getId());
            // Map<String, Object> adDetails = new java.util.HashMap<>(); // Simulate adDetails map
            // ... rest of simulation block removed ...
            // --- END SIMULATION REMOVAL ---

            // --- STEP 2: Extract data from parsed JSON --- 
            if (adDetails.isEmpty()) {
                log.warn("Parsed 'adDetails' from JSON is empty for ID {}. No data to populate from adDetails.", property.getId());
                // Potentially return or throw to indicate that essential data source is missing
            }
            if (adUserData.isEmpty()) {
                log.warn("Parsed 'adUserData' from JSON is empty for ID {}.", property.getId());
            }

            property.setTitle((String) adDetails.get("title"));
            log.debug("Title from JSON for ID {}: {}", property.getId(), property.getTitle());

            property.setDescription((String) adDetails.get("description"));
            if (property.getDescription()!=null) {
                log.debug("Description from JSON for ID {}: ({} chars)", property.getId(), property.getDescription().length());
            } else {
                log.debug("Description from JSON for ID {} is null", property.getId());
            }

            Object priceObj = adDetails.get("price");
            String extractedPrice = null;
            if (priceObj != null) {
                extractedPrice = extractNumericPrice(priceObj.toString());
            }

            if (extractedPrice != null && !extractedPrice.isEmpty()) {
                property.setPrice(extractedPrice);
                log.debug("Price from JSON for ID {}: {} (numeric) from raw: {}", property.getId(), property.getPrice(), priceObj != null ? priceObj.toString() : "N/A");
            } else {
                log.warn("Price not found or could not be extracted for ID {}. Setting price to '0'. Raw price object was: {}", property.getId(), priceObj != null ? priceObj.toString() : "null");
                property.setPrice("0"); // Default to "0"
            }

            property.setContactPhone((String) adDetails.get("phone"));
            log.debug("Contact Phone from JSON for ID {}: {}", property.getId(), property.getContactPhone());
            
            @SuppressWarnings("unchecked")
            List<String> images = (List<String>) adDetails.get("images");
            if (images != null && !images.isEmpty()) {
                property.setMainImageUrl(images.get(0));
                property.setImageUrls(new ArrayList<>(images)); 
                log.debug("Images from JSON for ID {}: Main='{}', Count={}", property.getId(), property.getMainImageUrl(), property.getImageUrls().size());
            } else {
                log.warn("No images found in JSON for ID {}", property.getId());
            }

            // Location details
            @SuppressWarnings("unchecked")
            Map<String, Object> locationMap = (Map<String, Object>) adDetails.get("location");
            String delegation = null;
            String governorate = null;
            if (locationMap != null) {
                delegation = (String) locationMap.get("delegation");
                governorate = (String) locationMap.get("governorate");
                property.setLocationCity(governorate); 
                property.setLocationDelegation(delegation);
                log.debug("Location from JSON for ID {}: Delegation='{}', Governorate='{}'", property.getId(), delegation, governorate);
            }
            String adUserAddress = (String) adUserData.get("address");
            property.setFullAddress(buildAddressLogic(adUserAddress, delegation, governorate));
            log.debug("Built Full Address for ID {}: {}", property.getId(), property.getFullAddress());

            // Ad Params (for rooms/bedrooms if not from title/S+N pattern later)
            // This parsing for rooms/bedrooms from adParams will be used if S+N pattern fails
            if (property.getRooms() == null && adParams != null) { // Check if rooms not already set by S+N
                for (Map<String, Object> param : adParams) {
                    String label = (String) param.get("label");
                    String value = (String) param.get("value");
                    if ("Chambres".equalsIgnoreCase(label) && value != null) {
                        try {
                            int chambres = Integer.parseInt(value);
                            property.setBedrooms(chambres); 
                            // If title suggests it's a studio, rooms = bedrooms, else rooms = bedrooms + 1 (for salon)
                            if (property.getTitle() != null && property.getTitle().toLowerCase().contains("studio")) {
                                 property.setRooms(chambres);
                            } else {
                                 property.setRooms(chambres + 1); 
                            }
                            log.info("Rooms/Bedrooms from JSON adParams for ID {}: Rooms={}, Bedrooms={}", property.getId(), property.getRooms(), property.getBedrooms());
                            break; // Found chambres, no need to check further params for this
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse 'Chambres' value '{}' to int for ID {}", value, property.getId());
                        }
                    }
                }
            }
            log.info("Successfully populated property ID {} from JSON data.", property.getId());

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse __NEXT_DATA__ JSON for ID {} due to Jackson parsing error: {}", property.getId(), e.getMessage(), e);
        } catch (Exception e) { // Catch broader exceptions if JSON parsing itself fails with real parser
            log.error("Critical error during JSON processing or data extraction for ID {}: {}", property.getId(), e.getMessage(), e);
            // Indicate that JSON parsing failed so selector fallbacks might be needed for all fields
             // (Handled by jsonParsedSuccessfully flag in extractData method)
        }
    }

    private String extractNumericPrice(String rawPrice) {
        if (rawPrice == null || rawPrice.isEmpty()) {
            return null;
        }
        // Removes all non-digit characters. Handles cases like "1.000 DT", "1000DT", "Price: 500"
        String numericPrice = rawPrice.replaceAll("[^0-9]", ""); // Using [0-9] instead of \\d to be absolutely safe, though \\d should also work.
        if (numericPrice.isEmpty()) {
            log.warn("Could not extract numeric value from price string: {}", rawPrice);
            return null;
        }
        log.debug("Extracted numeric price '{}' from raw string '{}'", numericPrice, rawPrice);
        return numericPrice;
    }

    private String buildAddressLogic(String adUserAddress, String delegation, String governorate) {
        StringBuilder addressBuilder = new StringBuilder();
        if (adUserAddress != null && !adUserAddress.trim().isEmpty()) {
            addressBuilder.append(adUserAddress.trim());
        }
        // Append delegation and governorate if they are not already in the adUserAddress
        if (delegation != null && !delegation.trim().isEmpty()) {
            if (addressBuilder.length() > 0 && !adUserAddress.toLowerCase().contains(delegation.toLowerCase())) {
                addressBuilder.append(", ").append(delegation.trim());
            } else if (addressBuilder.length() == 0) {
                addressBuilder.append(delegation.trim());
            }
        }
        if (governorate != null && !governorate.trim().isEmpty()) {
            if (addressBuilder.length() > 0 && !addressBuilder.toString().toLowerCase().contains(governorate.toLowerCase())) {
                addressBuilder.append(", ").append(governorate.trim());
            } else if (addressBuilder.length() == 0) {
                addressBuilder.append(governorate.trim());
            }
        }
        if (addressBuilder.length() > 0 && !addressBuilder.toString().toLowerCase().contains("tunisia")) {
             addressBuilder.append(", Tunisia"); // Add country for geocoding context
        }
        String finalAddress = addressBuilder.toString().trim();
        log.debug("Built address: {}", finalAddress);
        return finalAddress.isEmpty() ? null : finalAddress;
    }

    // Helper method to parse S+N, SN, S N patterns from title or description
    private void parseRoomsAndBedrooms(TayaraProperty property) {
        String title = property.getTitle();
        String description = property.getDescription(); // Assuming description is populated by now
        String textToSearch = (title != null ? title : "") + " " + (description != null ? description : "");

        if (textToSearch.trim().isEmpty()) {
            log.debug("Title and description are empty for ID {}, cannot parse rooms/bedrooms.", property.getId());
            return;
        }

        // Log only a snippet to avoid overly long log messages
        String logSnippet = textToSearch.length() > 100 ? textToSearch.substring(0, 100) + "..." : textToSearch;
        log.debug("Parsing rooms/bedrooms from text snippet: '{}' for ID {}", logSnippet, property.getId());

        // Pattern: S+N, S N, SN (e.g., S+3, S 3, S3)
        // S+N or S N means N Bedrooms, so N+1 total Rooms.
        // Regex: [Ss] followed by optional whitespace, optional '+', optional whitespace, then one or more digits.
        Pattern sPattern = Pattern.compile("[Ss]\\\\s*\\\\+?\\\\s*(\\\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher sMatcher = sPattern.matcher(textToSearch);

        if (sMatcher.find()) {
            try {
                int n = Integer.parseInt(sMatcher.group(1));
                property.setBedrooms(n);
                property.setRooms(n + 1); // S+N implies N bedrooms and 1 living room
                log.info("Parsed from S+N pattern for ID {}: Rooms={}, Bedrooms={}", property.getId(), property.getRooms(), property.getBedrooms());
                return; // Found and parsed, so exit
            } catch (NumberFormatException e) {
                log.warn("Could not parse number from S+N pattern for ID {}: {}", property.getId(), sMatcher.group(1));
            }
        }

        // Add other patterns if needed, e.g., looking for "X chambres" from adParams in JSON later
        log.debug("S+N pattern not found for ID {}. Rooms/Bedrooms not set from title/description parsing.", property.getId());
    }
    
    // Helper method to check if the announcement is recent enough
    private boolean isRecentEnough(String ageString) {
        if (ageString == null || ageString.trim().isEmpty()) {
            log.warn("Age string is null or empty, cannot determine recency. Assuming not recent by default for safety.");
            return false; 
        }

        String normalizedAgeString = ageString.toLowerCase()
                                             .replace("il y a", "") 
                                             .replace("à l'instant", "0 minute ago") // Corrected: Use plain apostrophe if that's the source HTML, ensure singular for regex consistency
                                             .trim();
        
        log.debug("Normalized age string for recency check: '{}' from original: '{}'", normalizedAgeString, ageString);

        if (normalizedAgeString.contains("aujourd'hui")) { // Corrected: plain apostrophe
            log.debug("Parsed as 'aujourd'hui', considered recent.");
            return true; 
        }
        if (normalizedAgeString.contains("hier")) {
            log.debug("Parsed as 'hier', considered recent.");
            return true; 
        }
        
        // Handle "an hour/day/week/month ago" or "une heure/jour/semaine/mois"
        if (normalizedAgeString.contains("an hour ago") || normalizedAgeString.contains("une heure")) {
            log.debug("Parsed as 'an hour ago' or 'une heure', considered recent (0 days).");
            return true; // Hours are always recent enough
        }
        if (normalizedAgeString.contains("a day ago") || normalizedAgeString.contains("un jour")) {
            log.debug("Parsed as 'a day ago' or 'un jour', considered recent (1 day).");
            return 1 <= MAX_ANNOUNCEMENT_AGE_DAYS;
        }
        if (normalizedAgeString.contains("a week ago") || normalizedAgeString.contains("une semaine")) {
            log.debug("Parsed as 'a week ago' or 'une semaine', considered recent (7 days).");
            return 7 <= MAX_ANNOUNCEMENT_AGE_DAYS;
        }
        if (normalizedAgeString.contains("a month ago") || normalizedAgeString.contains("un mois")) {
            log.debug("Parsed as 'a month ago' or 'un mois', considered recent (30 days).");
            return 30 <= MAX_ANNOUNCEMENT_AGE_DAYS; 
        }

        // Aggressively try to isolate the "X unit ago" part for the regex
        String coreAgeString = normalizedAgeString;
        // If there's a comma, assume the relevant age part is after the last comma.
        // e.g., "monastir , 3 hours ago" -> "3 hours ago"
        // e.g., "quelque chose, autre chose, 3 hours ago" -> "3 hours ago"
        int lastCommaIndex = coreAgeString.lastIndexOf(',');
        if (lastCommaIndex != -1 && lastCommaIndex + 1 < coreAgeString.length()) {
            coreAgeString = coreAgeString.substring(lastCommaIndex + 1).trim();
        }
        log.debug("Core age string for regex attempt: '{}' (from normalized: '{}')", coreAgeString, normalizedAgeString);

        // Regex to find patterns like "23 days ago", "3 weeks ago", "2 mois", "5 hours ago"
        // This regex now attempts to match the *entire* coreAgeString.
        // Java String needs \\d for regex \d, and \\s for regex \s.
        // Added English units: hour(s), day(s), week(s), month(s)
        Pattern pattern = Pattern.compile("^(\\d+)\\s*(minute|minutes|hour|hours|heure|heures|day|days|jour|jours|week|weeks|semaine|semaines|month|months|mois)(?:\\s+ago)?$");
        Matcher matcher = pattern.matcher(coreAgeString); 

        if (matcher.matches()) { // Use .matches() for full string match on the isolated coreAgeString
            try {
                int value = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2).toLowerCase(); // Normalize unit to lower case for switch
                int daysAgo = 0;

                switch (unit) {
                    case "minute":
                    case "minutes":
                    case "hour":  // Added English singular
                    case "hours": // Added English plural
                    case "heure":
                    case "heures":
                        daysAgo = 0; 
                        break;
                    case "day":   // Added English singular
                    case "days":  // Added English plural
                    case "jour":
                    case "jours":
                        daysAgo = value;
                        break;
                    case "week":   // Added English singular
                    case "weeks":  // Added English plural
                    case "semaine":
                    case "semaines":
                        daysAgo = value * 7;
                        break;
                    case "month":  // Added English singular
                    case "months": // Added English plural
                    case "mois":
                        daysAgo = value * 30; 
                        break;
                    default:
                        log.warn("Unknown unit in age string: '{}'. Full string: '{}'", unit, ageString);
                        return false; 
                }
                log.debug("Parsed by regex: {} days ago from string '{}' (value: {}, unit: {})", daysAgo, ageString, value, unit);
                return daysAgo <= MAX_ANNOUNCEMENT_AGE_DAYS;
            } catch (NumberFormatException e) {
                log.warn("Could not parse number from regex in age string: '{}'. Full string: '{}'", matcher.group(1), ageString);
                return false; 
            }
        } else {
            log.warn("Could not parse core age string format via regex: '{}'. Original normalized string: '{}'. Common formats: 'X jours/semaines/mois', 'Hier', 'Aujourd\'hui', 'a month ago'.", coreAgeString, normalizedAgeString);
            return false; 
        }
    }

    private String extractText(Page page, String selector) {
        if (selector == null || selector.trim().isEmpty()) {
            log.warn("Selector is null or empty, cannot extract text.");
            return null;
        }
        try {
            ElementHandle element = page.querySelector(selector);
            if (element != null) {
                String text = element.textContent();
                return text != null ? text.trim() : null;
            } else {
                log.warn("Element not found for selector: {}", selector);
            }
        } catch (Exception e) {
            // Ensuring that if the selector itself has {} it doesn't break SLF4J
            log.warn("Could not extract text for selector '{}' from page {}: {}", selector.replace("{", "_LCURLY_").replace("}", "_RCURLY_"), page.url(), e.getMessage());
        }
        return null;
    }
    
    // TODO: Implement other extraction methods:
    // private String extractPrice(Page page, String selector) { ... }
    // private String extractPhoneNumberWithInteraction(Page page, String clickSelector, String inputSelector, String submitSelector, String revealedSelector) { ... }
    // private void extractImages(Page page, TayaraProperty property, String mainImageSelector, String galleryItemSelector) { ... }
    // private void parseRoomsAndBedrooms(TayaraProperty property) { ... } // Based on title/description
    // private void buildFullAddress(TayaraProperty property, Page page, String... locationSelectors) { ... }
    // private void geocodeProperty(TayaraProperty property) { ... } // Using GeocodingService if available

    @Override
    public String getScraperName() {
        return "PlaywrightTayaraScraper";
    }

    protected Browser getBrowser(Playwright playwright) {
        // This is a placeholder. In a real scenario, this would come from AbstractPlaywrightScraper
        // or be configured based on properties (e.g., headless, browser type).
        log.debug("AbstractPlaywrightScraper.getBrowser() called - using Chromium headless by default for this example.");
        return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }
    protected static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
} 