package com.example.spring_security.service;

import com.example.spring_security.dao.InstituteRepository;
import com.example.spring_security.dao.PropertyListingRepository;
import com.example.spring_security.dto.PropertySearchCriteria;
import com.example.spring_security.dto.PropertyListingDTO;
import com.example.spring_security.dto.PropertyListingCreateDTO;
import com.example.spring_security.dto.PropertyListingUpdateDTO;
import com.example.spring_security.model.Institute;
import com.example.spring_security.model.PropertyListing;
import com.example.spring_security.model.User; // Assuming User model exists
import com.example.spring_security.exception.GeocodingException;
import com.example.spring_security.service.GeocodingService; // Correct import
import com.example.spring_security.exception.ResourceNotFoundException; // Assuming you have this or similar
import org.springframework.security.access.AccessDeniedException; // For authorization

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal; // Ensure BigDecimal is imported
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true) // Default to read-only for service methods
public class PropertyListingService {

    private static final Logger logger = LoggerFactory.getLogger(PropertyListingService.class);

    private final PropertyListingRepository propertyListingRepository;
    private final InstituteRepository instituteRepository;
    private final ModelMapper modelMapper;
    private final GeocodingService geocodingService; // Inject GeocodingService
    // private final UserService userService; // For fetching current user, add when needed

    private static final int SRID_WGS84 = 4326; // Standard SRID for WGS 84 (lat/lon)

    @Autowired
    public PropertyListingService(PropertyListingRepository propertyListingRepository,
                                  InstituteRepository instituteRepository,
                                  ModelMapper modelMapper,
                                  GeocodingService geocodingService) { // Add GeocodingService
        this.propertyListingRepository = propertyListingRepository;
        this.instituteRepository = instituteRepository;
        this.modelMapper = modelMapper;
        this.geocodingService = geocodingService;
    }

    public Page<PropertyListingDTO> searchProperties(PropertySearchCriteria criteria, Pageable pageable) {
        logger.debug("Searching properties with criteria: {} and pageable: {}", criteria, pageable);

        Specification<PropertyListing> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // This will hold the geography expression of the institute if proximity search is active
            // It's declared here to be accessible for ordering if needed.
            Expression<?> instituteGeographyFinal = null;

            if (criteria.getPropertyType() != null && !criteria.getPropertyType().trim().isEmpty()) {
                logger.trace("Adding property type predicate: {}", criteria.getPropertyType());
                predicates.add(cb.equal(cb.lower(root.get("propertyType")), criteria.getPropertyType().toLowerCase()));
            }
            if (criteria.getMinPrice() != null) {
                logger.trace("Adding min price predicate: {}", criteria.getMinPrice());
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.getMinPrice()));
            }
            if (criteria.getMaxPrice() != null) {
                logger.trace("Adding max price predicate: {}", criteria.getMaxPrice());
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.getMaxPrice()));
            }

            // Added: Bedrooms filter (exact match)
            if (criteria.getBedrooms() != null && criteria.getBedrooms() > 0) {
                logger.trace("Adding bedrooms predicate: {}", criteria.getBedrooms());
                predicates.add(cb.equal(root.get("bedrooms"), criteria.getBedrooms()));
            }

            // Added: Area range filter
            if (criteria.getMinArea() != null) {
                logger.trace("Adding min area predicate: {}", criteria.getMinArea());
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), criteria.getMinArea()));
            }
            if (criteria.getMaxArea() != null) {
                logger.trace("Adding max area predicate: {}", criteria.getMaxArea());
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), criteria.getMaxArea()));
            }

            // Proximity search logic
            if (criteria.getInstituteId() != null && (criteria.getRadiusKm() != null && criteria.getRadiusKm() > 0)) {
                logger.trace("Attempting proximity search for instituteId: {} within radiusKm: {}", criteria.getInstituteId(), criteria.getRadiusKm());
                Optional<Institute> instituteOpt = instituteRepository.findById(criteria.getInstituteId());
                if (instituteOpt.isPresent()) {
                    Institute institute = instituteOpt.get();
                    logger.debug("Found institute: {} for proximity search at lon: {}, lat: {}", 
                                 institute.getName(), institute.getLongitude(), institute.getLatitude());

                    // Convert km to meters for spatial query
                    double radiusInMeters = criteria.getRadiusKm() * 1000.0;
                    
                    // Log actual radius being used for debugging
                    logger.info("Using radius of {} meters ({}km) for proximity search around {}", 
                              radiusInMeters, criteria.getRadiusKm(), institute.getName());

                    // Construct geography point for the property listing from its longitude and latitude columns
                    Expression<?> propertyPoint = cb.function("ST_SetSRID", Object.class,
                            cb.function("ST_MakePoint", Object.class, root.get("longitude"), root.get("latitude")),
                            cb.literal(SRID_WGS84)
                    );
                    Expression<?> propertyGeography = cb.function("geography", Object.class, propertyPoint);

                    // Construct geography point for the institute from its literal longitude and latitude
                    Expression<?> institutePoint = cb.function("ST_SetSRID", Object.class,
                            cb.function("ST_MakePoint", Object.class, cb.literal(institute.getLongitude()), cb.literal(institute.getLatitude())),
                            cb.literal(SRID_WGS84)
                    );
                    instituteGeographyFinal = cb.function("geography", Object.class, institutePoint);

                    // Create the ST_DWithin predicate - ensure properties are within the specified radius
                    Predicate distancePredicate = cb.isTrue(
                        cb.function("ST_DWithin", Boolean.class,
                            propertyGeography,
                            instituteGeographyFinal,
                            cb.literal(radiusInMeters),
                            cb.literal(true) // Use spheroid for more accurate earth distance calculations
                        )
                    );
                    predicates.add(distancePredicate);
                    logger.trace("Added ST_DWithin predicate for institute {} with radius {}m.", institute.getName(), radiusInMeters);

                } else {
                    logger.warn("Institute with ID: {} not found for proximity search. No results will be returned due to this filter.", criteria.getInstituteId());
                    predicates.add(cb.disjunction()); 
                }
            } else if (criteria.getInstituteId() != null) {
                // If we have an institute ID but no radiusKm, we still want to allow sorting by distance
                // This is useful for general searches where you want to sort by distance but don't need to filter by radius
                Optional<Institute> instituteOpt = instituteRepository.findById(criteria.getInstituteId());
                if (instituteOpt.isPresent()) {
                    Institute institute = instituteOpt.get();
                    logger.debug("Found institute: {} for distance calculation (no radius filter) at lon: {}, lat: {}", 
                                institute.getName(), institute.getLongitude(), institute.getLatitude());
                    
                    // Create the institute geography point for potential sorting by distance
                    Expression<?> institutePoint = cb.function("ST_SetSRID", Object.class,
                            cb.function("ST_MakePoint", Object.class, cb.literal(institute.getLongitude()), cb.literal(institute.getLatitude())),
                            cb.literal(SRID_WGS84)
                    );
                    instituteGeographyFinal = cb.function("geography", Object.class, institutePoint);
                }
            }

            query.where(cb.and(predicates.toArray(new Predicate[0])));

            // ORDER BY Logic: Handle sorting by distance if requested and applicable
            List<Order> orders = new ArrayList<>();
            if (pageable.getSort().isSorted()) {
                for (Sort.Order sortOrder : pageable.getSort()) {
                    if ("distance".equalsIgnoreCase(sortOrder.getProperty())) {
                        if (instituteGeographyFinal != null) {
                            // Recalculate propertyGeography for ST_Distance, as it might be specific to where clause
                            Expression<?> propertyPointForSort = cb.function("ST_SetSRID", Object.class,
                                    cb.function("ST_MakePoint", Object.class, root.get("longitude"), root.get("latitude")),
                                    cb.literal(SRID_WGS84));
                            Expression<?> propertyGeographyForSort = cb.function("geography", Object.class, propertyPointForSort);

                            Expression<Double> distanceExpression = cb.function("ST_Distance", Double.class,
                                    propertyGeographyForSort,
                                    instituteGeographyFinal // Use the institute's geography previously calculated
                            );
                            orders.add(sortOrder.isAscending() ? cb.asc(distanceExpression) : cb.desc(distanceExpression));
                            logger.debug("Applying sort by distance: {}", sortOrder.getDirection());
                        } else {
                            logger.warn("Sort by 'distance' requested but no institute context available. Falling back to sorting by listingDate.");
                            // Fall back to sorting by listingDate
                            orders.add(cb.desc(root.get("listingDate")));
                        }
                    } else {
                        // Standard property sorting
                        try {
                           orders.add(sortOrder.isAscending() ? cb.asc(root.get(sortOrder.getProperty())) : cb.desc(root.get(sortOrder.getProperty())));
                        } catch (IllegalArgumentException e) {
                            logger.warn("Invalid sort property '{}' requested. Falling back to listingDate sort. Error: {}", sortOrder.getProperty(), e.getMessage());
                            // Fall back to a default sort
                            orders.add(cb.desc(root.get("listingDate")));
                        }
                    }
                }
            }
            
            // If no valid sort orders were added, apply a default sort
            if (orders.isEmpty()) {
                orders.add(cb.desc(root.get("listingDate")));
                logger.debug("Applying default sort by listingDate DESC");
            }
            
            query.orderBy(orders);

            return query.getRestriction(); // Return the combined predicates for the WHERE clause
        };
        Page<PropertyListing> propertyPage = propertyListingRepository.findAll(spec, pageable);
        return propertyPage.map(this::convertToDto);
    }

    public Optional<PropertyListingDTO> getPropertyById(Long id) {
        logger.debug("Fetching property by ID: {}", id);
        return propertyListingRepository.findById(id)
                                      .map(this::convertToDto);
    }

    // Helper method to convert PropertyListing to PropertyListingDTO
    private PropertyListingDTO convertToDto(PropertyListing propertyListing) {
        PropertyListingDTO dto = modelMapper.map(propertyListing, PropertyListingDTO.class);
        if (propertyListing.getUser() != null) {
            dto.setOwnerUsername(propertyListing.getUser().getUsername()); // Assuming User has getUsername()
        }
        if (propertyListing.getImageUrls() != null && !propertyListing.getImageUrls().isEmpty()) {
            String imageUrl = propertyListing.getImageUrls().get(0);
            logger.info("Original image URL for property {}: '{}'", propertyListing.getId(), imageUrl);
            // Fix image URLs that may have been double-encoded with @ prefix
            if (imageUrl != null && imageUrl.startsWith("@")) {
                imageUrl = imageUrl.substring(1);
                logger.info("Cleaned image URL: '{}'", imageUrl);
            }
            dto.setMainImageUrl(imageUrl);
        } else {
            dto.setMainImageUrl(null); // Or a placeholder image URL
            logger.info("No image URLs found for property {}", propertyListing.getId());
        }
        return dto;
    }

    @Transactional // Override to make it read-write
    public PropertyListingDTO createProperty(PropertyListingCreateDTO createDto, User currentUser) {
        logger.info("User {} attempting to create property with title: {}", currentUser.getUsername(), createDto.getTitle());

        PropertyListing propertyListing = modelMapper.map(createDto, PropertyListing.class);

        // Set owner
        propertyListing.setUser(currentUser);

        // Set defaults
        propertyListing.setListingDate(LocalDateTime.now());
        propertyListing.setActive(true); // Default to active, can be changed later

        // Geocode address
        if (createDto.getFullAddress() != null && !createDto.getFullAddress().trim().isEmpty()) {
            try {
                double[] coordinates = geocodingService.geocode(createDto.getFullAddress());
                if (coordinates != null && coordinates.length == 2) {
                    propertyListing.setLatitude(coordinates[0]);
                    propertyListing.setLongitude(coordinates[1]);
                    logger.info("Geocoded address '{}' to Lat: {}, Lon: {}", createDto.getFullAddress(), coordinates[0], coordinates[1]);
                } else {
                    logger.warn("Geocoding for address '{}' returned null or invalid coordinates. Property will be saved without coordinates.", createDto.getFullAddress());
                    // Optionally, you could throw an exception here if coordinates are strictly required
                    // Or set to a default/null if your DB schema allows
                    propertyListing.setLatitude(null);
                    propertyListing.setLongitude(null);
                }
            } catch (Exception e) {
                // Catching a broad exception from geocoding service for now.
                // Consider a more specific exception from GeocodingService if it can throw one.
                logger.error("Error during geocoding for address '{}': {}. Property will be saved without coordinates.", createDto.getFullAddress(), e.getMessage());
                propertyListing.setLatitude(null);
                propertyListing.setLongitude(null);
                // Depending on requirements, you might rethrow or wrap this in a custom business exception
                // For example: throw new GeocodingException("Failed to geocode address: " + createDto.getFullAddress(), e);
            }
        }

        PropertyListing savedProperty = propertyListingRepository.save(propertyListing);
        logger.info("Property created successfully with ID: {} by User: {}", savedProperty.getId(), currentUser.getUsername());
        
        return convertToDto(savedProperty);
    }

    @Transactional // Override to make it read-write
    public PropertyListingDTO updateProperty(Long id, PropertyListingUpdateDTO updateDto, User currentUser) {
        logger.info("User {} attempting to update property with ID: {}", currentUser.getUsername(), id);

        PropertyListing propertyListing = propertyListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PropertyListing not found with id: " + id));

        // Authorization: Check if the current user is the owner or an admin
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN; // Use getRole() and direct enum comparison

        // Ensure User.getId() returns a primitive for direct comparison, or use .equals() if it returns an Integer object.
        // Assuming User.getId() returns int based on previous context.
        if (propertyListing.getUser() == null || (propertyListing.getUser().getId() != currentUser.getId() && !isAdmin)) {
            logger.warn("Access denied for User {} to update property ID: {}. User is not owner or admin.", currentUser.getUsername(), id);
            throw new AccessDeniedException("You are not authorized to update this property.");
        }

        // Preserve original creation date and owner unless specifically changed by admin logic (not implemented here)
        LocalDateTime originalListingDate = propertyListing.getListingDate();
        User originalOwner = propertyListing.getUser();

        // Map only non-null fields from DTO to entity to support partial updates
        modelMapper.getConfiguration().setSkipNullEnabled(true); // Ensure nulls are skipped
        modelMapper.map(updateDto, propertyListing);
        modelMapper.getConfiguration().setSkipNullEnabled(false); // Reset after mapping

        // Restore potentially overwritten fields if they were not part of the DTO
        propertyListing.setListingDate(originalListingDate); // Keep original listing date
        propertyListing.setUser(originalOwner); // Keep original owner

        // Handle address change and re-geocoding if address is provided and different
        if (updateDto.getFullAddress() != null && !updateDto.getFullAddress().trim().isEmpty() &&
            !updateDto.getFullAddress().equals(propertyListing.getFullAddress())) { // Check if address actually changed
            logger.info("Address changed for property ID: {}. Re-geocoding.", id);
            try {
                double[] coordinates = geocodingService.geocode(updateDto.getFullAddress());
                if (coordinates != null && coordinates.length == 2) {
                    propertyListing.setLatitude(coordinates[0]);
                    propertyListing.setLongitude(coordinates[1]);
                    logger.info("Re-geocoded address for property ID: {} to Lat: {}, Lon: {}", id, coordinates[0], coordinates[1]);
                } else {
                    logger.warn("Re-geocoding for new address '{}' for property ID: {} returned null or invalid coordinates.", updateDto.getFullAddress(), id);
                    // Decide if old coordinates should be kept or nulled. For now, keeping old ones if new geocoding fails.
                }
            } catch (GeocodingException e) {
                logger.error("Error during re-geocoding for property ID: {}: {}. Keeping original coordinates.", id, e.getMessage());
                // Optionally re-throw or handle more gracefully
            }
        } else if (updateDto.getFullAddress() != null && updateDto.getFullAddress().trim().isEmpty()) {
            // If address is explicitly set to empty, null out coordinates
            propertyListing.setLatitude(null);
            propertyListing.setLongitude(null);
            propertyListing.setFullAddress(null); // also clear the full address field
             logger.info("Address explicitly cleared for property ID: {}. Coordinates nulled.", id);
        }


        PropertyListing updatedProperty = propertyListingRepository.save(propertyListing);
        logger.info("Property ID: {} updated successfully by User: {}", id, currentUser.getUsername());
        return convertToDto(updatedProperty);
    }

    @Transactional // Override to make it read-write
    public void deleteProperty(Long id, User currentUser) {
        logger.info("User {} attempting to delete property with ID: {}", currentUser.getUsername(), id);

        PropertyListing propertyListing = propertyListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PropertyListing not found with id: " + id));

        // Authorization: Check if the current user is the owner or an admin
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN; // Use getRole() and direct enum comparison
        
        // Assuming User.getId() returns int based on previous context.
        if (propertyListing.getUser() == null || (propertyListing.getUser().getId() != currentUser.getId() && !isAdmin)) {
            logger.warn("Access denied for User {} to delete property ID: {}. User is not owner or admin.", currentUser.getUsername(), id);
            throw new AccessDeniedException("You are not authorized to delete this property.");
        }

        propertyListingRepository.delete(propertyListing);
        logger.info("Property ID: {} deleted successfully by User: {}", id, currentUser.getUsername());
    }
} 