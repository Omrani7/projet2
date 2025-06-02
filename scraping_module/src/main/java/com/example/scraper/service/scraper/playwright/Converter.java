package com.example.scraper.service.scraper.playwright;

import com.example.scraper.dto.PropertyDto;
import com.example.scraper.model.ImmobilierProperty;
import com.example.scraper.model.PropertyListing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for converting between scraper-specific models and the general PropertyListing model
 */
public class Converter {
    
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+[,\\s]*\\d*)");
    
    /**
     * Convert an ImmobilierProperty to a PropertyListing
     *
     * @param property The source property from Immobilier scraper
     * @return A PropertyListing entity
     */
    public static PropertyListing toPropertyListing(ImmobilierProperty property) {
        if (property == null) {
            return null;
        }
        
        PropertyListing listing = new PropertyListing();
        
        listing.setTitle(property.getTitle());
        listing.setDescription(property.getDescription());
        
        // Extract numeric price
        if (property.getPrice() != null) {
            Matcher matcher = PRICE_PATTERN.matcher(property.getPrice());
            if (matcher.find()) {
                String priceStr = matcher.group(1).replaceAll("[^\\d.]", "");
                try {
                    listing.setPrice(new BigDecimal(priceStr));
                } catch (NumberFormatException e) {
                    listing.setPrice(BigDecimal.ZERO);
                }
            }
        }
        
        // Location data
        listing.setLocation(property.getLocation());
        listing.setCity(property.getCity());
        listing.setDistrict(property.getDistrict());
        listing.setFullAddress(property.getFullAddress());
        listing.setLatitude(property.getLatitude());
        listing.setLongitude(property.getLongitude());
        listing.setFormattedAddress(property.getFormattedAddress());
        
        // Extract surface/area
        if (property.getSurface() != null) {
            Matcher matcher = PRICE_PATTERN.matcher(property.getSurface());
            if (matcher.find()) {
                String areaStr = matcher.group(1).replaceAll("[^\\d.]", "");
                try {
                    listing.setArea(Double.parseDouble(areaStr));
                } catch (NumberFormatException e) {
                    // Ignore parse errors
                }
            }
        }
        
        // Property details
        listing.setRooms(property.getRooms());
        listing.setBedrooms(property.getBedrooms());
        listing.setBathrooms(property.getBathrooms());
        listing.setPropertyType(property.getType());
        listing.setContactInfo(property.getContactPhone());
        
        // Images
        if (property.getImageUrls() != null) {
            listing.getImageUrls().addAll(property.getImageUrls());
        }
        
        // Source info
        listing.setSourceUrl(property.getUrl());
        listing.setSourceWebsite("immobilier.com.tn");
        listing.setListingDate(LocalDateTime.now());
        listing.setActive(true);
        
        return listing;
    }
    
    /**
     * Convert a PropertyListing to a PropertyDto
     *
     * @param listing The source PropertyListing entity
     * @return A PropertyDto object
     */
    public static PropertyDto toPropertyDto(PropertyListing listing) {
        if (listing == null) {
            return null;
        }
        
        return PropertyDto.builder()
            .id(listing.getId() != null ? listing.getId().toString() : null)
            .title(listing.getTitle())
            .description(listing.getDescription())
            .price(listing.getPrice() != null ? listing.getPrice().toString() : null)
            .location(listing.getLocation())
            .area(listing.getArea() != null ? listing.getArea().toString() : null)
            .propertyType(listing.getPropertyType())
            .contactInfo(listing.getContactInfo())
            .city(listing.getCity())
            .district(listing.getDistrict())
            .fullAddress(listing.getFullAddress())
            .latitude(listing.getLatitude())
            .longitude(listing.getLongitude())
            .formattedAddress(listing.getFormattedAddress())
            .rooms(listing.getRooms())
            .bedrooms(listing.getBedrooms())
            .bathrooms(listing.getBathrooms())
            .imageUrls(listing.getImageUrls())
            .sourceUrl(listing.getSourceUrl())
            .sourceWebsite(listing.getSourceWebsite())
            .listingDate(listing.getListingDate())
            .build();
    }

    /**
     * Convert a TayaraProperty to a PropertyListing
     *
     * @param property The source property from Tayara scraper
     * @return A PropertyListing entity
     */
    public static PropertyListing toPropertyListing(com.example.scraper.model.TayaraProperty property) {
        if (property == null) {
            return null;
        }
        PropertyListing listing = new PropertyListing();
        listing.setTitle(property.getTitle());
        listing.setDescription(property.getDescription());
        // Extract numeric price
        if (property.getPrice() != null) {
            try {
                String priceStr = property.getPrice().replaceAll("[^\\d.]", "");
                listing.setPrice(priceStr.isEmpty() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(priceStr));
            } catch (NumberFormatException e) {
                listing.setPrice(java.math.BigDecimal.ZERO);
            }
        }
        listing.setLocation(property.getLocationCity());
        listing.setCity(property.getLocationCity());
        listing.setDistrict(property.getLocationDelegation());
        listing.setFullAddress(property.getFullAddress());
        listing.setLatitude(property.getLatitude());
        listing.setLongitude(property.getLongitude());
        listing.setRooms(property.getRooms());
        listing.setBedrooms(property.getBedrooms());
        listing.setContactInfo(property.getContactPhone());
        if (property.getImageUrls() != null) {
            listing.getImageUrls().addAll(property.getImageUrls());
        }
        listing.setSourceUrl(property.getUrl());
        listing.setSourceWebsite(property.getSourceWebsite());
        listing.setListingDate(java.time.LocalDateTime.now());
        listing.setActive(true);
        return listing;
    }
} 