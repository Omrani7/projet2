package com.example.scraper.service.scraper;

import com.example.scraper.model.PropertyListing;

import java.util.List;
import java.util.Map;

public interface Scraper {
    List<PropertyListing> scrape(Map<String, Object> parameters);
} 