package com.example.spring_security.controller;

import com.example.spring_security.model.PropertyListing;
import com.example.spring_security.service.PropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/properties/map")
@Slf4j
public class PropertyMapController {

    private final PropertyService propertyService;
    
    public PropertyMapController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }
    

    @GetMapping
    public String showPropertyMap(
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestParam(name = "minRooms", required = false) Integer minRooms,
            Model model) {
        
        List<PropertyListing> allProperties = propertyService.getAllProperties();
        
        List<PropertyListing> geoCodedProperties = allProperties.stream()
            .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
            .collect(Collectors.toList());
        
        if (maxPrice != null) {
            geoCodedProperties = geoCodedProperties.stream()
                .filter(p -> p.getPrice() != null && p.getPrice().doubleValue() <= maxPrice)
                .collect(Collectors.toList());
        }
        
        if (minRooms != null) {
            geoCodedProperties = geoCodedProperties.stream()
                .filter(p -> p.getRooms() != null && p.getRooms() >= minRooms)
                .collect(Collectors.toList());
        }
        
        model.addAttribute("properties", geoCodedProperties);
        log.info("Displaying {} geocoded properties on map", geoCodedProperties.size());
        
        return "property-map";
    }
} 