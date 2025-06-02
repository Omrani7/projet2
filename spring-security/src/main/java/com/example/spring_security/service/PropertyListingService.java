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
import com.example.spring_security.model.UserPrincipal;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.util.Objects;
import java.util.stream.Collectors;
import com.example.spring_security.dao.UserRepo;
import org.springframework.security.core.Authentication;
import com.example.spring_security.exception.UnauthorizedAccessException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Map;

@Service
@Transactional(readOnly = true) // Default to read-only for service methods
public class PropertyListingService {

    private static final Logger logger = LoggerFactory.getLogger(PropertyListingService.class);

    private final PropertyListingRepository propertyListingRepository;
    private final InstituteRepository instituteRepository;
    private final ModelMapper modelMapper;
    private final GeocodingService geocodingService; // Inject GeocodingService
    private final UserRepo userRepository;
    // private final UserService userService; // For fetching current user, add when needed

    private static final int SRID_WGS84 = 4326; // Standard SRID for WGS 84 (lat/lon)
    private final String UPLOAD_DIR = "uploads/owner-property-images/";

    @Autowired
    public PropertyListingService(PropertyListingRepository propertyListingRepository,
                                  InstituteRepository instituteRepository,
                                  ModelMapper modelMapper,
                                  GeocodingService geocodingService,
                                  UserRepo userRepository) { // Add GeocodingService and UserRepo
        this.propertyListingRepository = propertyListingRepository;
        this.instituteRepository = instituteRepository;
        this.modelMapper = modelMapper;
        this.geocodingService = geocodingService;
        this.userRepository = userRepository;
    }

    public Page<PropertyListingDTO> searchProperties(final PropertySearchCriteria criteria, final Pageable pageable) {
        logger.debug("Searching properties with criteria: {} and pageable: {}", criteria, pageable);

        // Handle "distance" sort separately
        Pageable effectivePageable = pageable;
        boolean sortByDistance = false;
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                if ("distance".equalsIgnoreCase(order.getProperty())) {
                    sortByDistance = true;
                    // Create a new pageable without the distance sort
                    Sort newSort = Sort.by(Sort.Direction.DESC, "listingDate"); // Default sort
                    effectivePageable = org.springframework.data.domain.PageRequest.of(
                            pageable.getPageNumber(),
                            pageable.getPageSize(),
                            newSort
                    );
                    break;
                }
            }
        }
        final Pageable finalEffectivePageableForLambda = effectivePageable;


        // Make this variable final so it can be used in lambda
        final boolean sortByDistanceFinal = sortByDistance;
        final Sort pageableSortForLambdaInSearch = pageable.getSort();
        final Sort effectivePageableSortForLambdaInSearch = finalEffectivePageableForLambda.getSort();

        Specification<PropertyListing> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // This will hold the geography expression of the institute if proximity search is active
            // It's declared here to be accessible for ordering if needed.
            Expression<?> instituteGeographyFinal = null;
            Expression<Double> distanceExpression = null;

            // Filter by active properties only (exclude deactivated/closed properties)
            predicates.add(cb.equal(root.get("active"), true));

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

            // Filter by property owner
            if (criteria.getOwnerId() != null) {
                logger.trace("Adding owner predicate: {}", criteria.getOwnerId());
                predicates.add(cb.equal(root.get("user").get("id"), criteria.getOwnerId()));
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
                    final Expression<?> instituteGeographyFinalInner = cb.function("geography", Object.class, institutePoint);

                    // Create the ST_DWithin predicate - ensure properties are within the specified radius
                    Predicate distancePredicate = cb.isTrue(
                        cb.function("ST_DWithin", Boolean.class,
                            propertyGeography,
                            instituteGeographyFinalInner,
                            cb.literal(radiusInMeters),
                            cb.literal(true) // Use spheroid for more accurate earth distance calculations
                        )
                    );
                    predicates.add(distancePredicate);
                    logger.trace("Added ST_DWithin predicate for institute {} with radius {}m.", institute.getName(), radiusInMeters);

                    // Calculate distance expression for sorting if needed
                    if (sortByDistanceFinal) {
                        distanceExpression = cb.function("ST_Distance", Double.class,
                                propertyGeography,
                                instituteGeographyFinalInner
                        );
                    }

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
                    final Expression<?> instituteGeographyFinalInner = cb.function("geography", Object.class, institutePoint);

                    // Calculate distance expression for sorting if needed
                    if (sortByDistanceFinal) {
                        Expression<?> propertyPointForSort = cb.function("ST_SetSRID", Object.class,
                                cb.function("ST_MakePoint", Object.class, root.get("longitude"), root.get("latitude")),
                                cb.literal(SRID_WGS84));
                        Expression<?> propertyGeographyForSort = cb.function("geography", Object.class, propertyPointForSort);

                        distanceExpression = cb.function("ST_Distance", Double.class,
                                propertyGeographyForSort,
                                instituteGeographyFinalInner
                        );
                    }
                }
            }

            query.where(cb.and(predicates.toArray(new Predicate[0])));

            // Handle sort by distance if requested and possible
            final Expression<Double> distanceExpressionFinalLambda = distanceExpression;
            if (sortByDistanceFinal && distanceExpressionFinalLambda != null) {
                for (Sort.Order order : pageableSortForLambdaInSearch) {
                    if ("distance".equalsIgnoreCase(order.getProperty())) {
                        query.orderBy(order.isAscending() ? cb.asc(distanceExpressionFinalLambda) : cb.desc(distanceExpressionFinalLambda));
                        logger.debug("Applied sorting by calculated distance: {}", order.getDirection());
                        break;
                    }
                }
            } else {
                // Apply standard sorting from effectivePageable
            List<Order> orders = new ArrayList<>();
                for (Sort.Order sortOrder : effectivePageableSortForLambdaInSearch) {
                    orders.add(sortOrder.isAscending() ? cb.asc(root.get(sortOrder.getProperty())) : cb.desc(root.get(sortOrder.getProperty())));
                }
                query.orderBy(orders);
            }

            return query.getRestriction();
        };
        
        Page<PropertyListing> propertyPage = propertyListingRepository.findAll(spec, finalEffectivePageableForLambda);
        
        // Debug log properties found and their source types
        logger.debug("Query found {} properties", propertyPage.getNumberOfElements());
        Map<String, Long> sourceTypeCounts = propertyPage.getContent().stream()
            .collect(Collectors.groupingBy(
                p -> p.getSourceType() != null ? p.getSourceType().toString() : "NULL",
                Collectors.counting()
            ));
        logger.info("Property counts by source type: {}", sourceTypeCounts);

        propertyPage.getContent().forEach(property -> {
            logger.debug("Found property ID: {}, Title: {}, SourceType: {}, Lat: {}, Long: {}",
                    property.getId(), property.getTitle(), property.getSourceType(), 
                    property.getLatitude(), property.getLongitude());
        });
        
        Page<PropertyListingDTO> dtoPage = propertyPage.map(this::convertToDto);
        
        if (sortByDistanceFinal) { // This refers to the sortByDistanceFinal declared before the lambda
            logger.debug("Sorted {} properties by distance", dtoPage.getNumberOfElements());
        }
        
        return dtoPage;
    }

    /**
     * Searches for properties with explicit handling of distance sorting.
     * This avoids the 'No property distance found' error by handling the sort in the query itself.
     * 
     * @param criteria The search criteria
     * @param pageable Pageable without the distance sort
     * @param distanceSortDirection Direction for distance sort if applicable (ASC or DESC)
     * @return A page of PropertyListingDTO objects
     */
    public Page<PropertyListingDTO> searchPropertiesWithDistanceSort(
            final PropertySearchCriteria criteria, 
            final Pageable pageable,
            final String distanceSortDirection) {
            
        logger.debug("Searching properties with criteria: {}, pageable: {}, distanceSort: {}", 
                    criteria, pageable, distanceSortDirection);
        
        final boolean sortByDistance = distanceSortDirection != null;
        final boolean sortAscending = "ASC".equalsIgnoreCase(distanceSortDirection);
        final Sort pageableSortForLambdaInSearchWithDist = pageable.getSort();
        
        // First, let's log all available OWNER properties for debugging
        List<PropertyListing> ownerProperties = propertyListingRepository.findAll((root, query, cb) -> {
            return cb.equal(root.get("sourceType"), PropertyListing.SourceType.OWNER);
        });
        logger.debug("Total OWNER properties in database: {}", ownerProperties.size());
        ownerProperties.forEach(prop -> {
            logger.debug("OWNER property - ID: {}, Lat: {}, Long: {}, Full Address: {}", 
                prop.getId(), prop.getLatitude(), prop.getLongitude(), prop.getFullAddress());
        });
        
        Specification<PropertyListing> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Expression<?> instituteGeographyTemp = null;
            Expression<Double> distanceExpressionTemp = null;
            
            // Debug log to confirm we're not filtering by source type
            logger.debug("Starting property search with no source type filtering");
            
            // Filter by active properties only (exclude deactivated/closed properties)
            predicates.add(cb.equal(root.get("active"), true));
            
            // Ensure properties have valid coordinates
            logger.debug("Adding coordinate validation predicates");
            predicates.add(cb.isNotNull(root.get("latitude")));
            predicates.add(cb.isNotNull(root.get("longitude")));
            predicates.add(cb.notEqual(root.get("latitude"), 0.0));
            predicates.add(cb.notEqual(root.get("longitude"), 0.0));
            
            // Apply standard filters
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
            if (criteria.getBedrooms() != null && criteria.getBedrooms() > 0) {
                logger.trace("Adding bedrooms predicate: {}", criteria.getBedrooms());
                predicates.add(cb.equal(root.get("bedrooms"), criteria.getBedrooms()));
            }
            if (criteria.getMinArea() != null) {
                logger.trace("Adding min area predicate: {}", criteria.getMinArea());
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), criteria.getMinArea()));
            }
            if (criteria.getMaxArea() != null) {
                logger.trace("Adding max area predicate: {}", criteria.getMaxArea());
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), criteria.getMaxArea()));
            }
            
            // Handle institute-based proximity search
            if (criteria.getInstituteId() != null) {
                Optional<Institute> instituteOpt = instituteRepository.findById(criteria.getInstituteId());
                if (instituteOpt.isPresent()) {
                    Institute institute = instituteOpt.get();
                    logger.debug("Found institute: {} for proximity search at lon: {}, lat: {}", 
                                institute.getName(), institute.getLongitude(), institute.getLatitude());
                    
                    // Add distance filter if radius specified
                    if (criteria.getRadiusKm() != null && criteria.getRadiusKm() > 0) {
                        double radiusInMeters = criteria.getRadiusKm() * 1000.0;
                        logger.info("Using radius of {} meters ({}km) for proximity search around {}", 
                                  radiusInMeters, criteria.getRadiusKm(), institute.getName());
                        
                        // Log raw SQL for debugging
                        logger.debug("Will apply spatial search with ST_DWithin using radius {}m", radiusInMeters);
                        
                        // Construct property geography for distance calculation - MODIFIED FOR BETTER COMPATIBILITY
                        Expression<?> propertyPoint = cb.function("ST_SetSRID", Object.class,
                                cb.function("ST_MakePoint", Object.class, 
                                    root.get("longitude"), 
                                    root.get("latitude")),
                                cb.literal(SRID_WGS84)
                        );
                        Expression<?> propertyGeography = cb.function("geography", Object.class, propertyPoint);
                        
                        // Construct institute geography point - MODIFIED FOR BETTER COMPATIBILITY
                        Expression<?> institutePoint = cb.function("ST_SetSRID", Object.class,
                                cb.function("ST_MakePoint", Object.class, 
                                    cb.literal(institute.getLongitude()), 
                                    cb.literal(institute.getLatitude())),
                                cb.literal(SRID_WGS84)
                        );
                        Expression<?> instituteGeography = cb.function("geography", Object.class, institutePoint);
                        
                        // Create the ST_DWithin predicate for radius filter - SIMPLIFIED
                        Predicate distancePredicate = cb.isTrue(
                            cb.function("ST_DWithin", Boolean.class,
                                propertyGeography,
                                instituteGeography,
                                cb.literal(radiusInMeters),
                                cb.literal(true) // Use spheroid for accurate earth distance
                            )
                        );
                        predicates.add(distancePredicate);
                        
                        // Update the reference for later use in distance sorting
                        instituteGeographyTemp = instituteGeography;
                        
                        // Output DEBUG-level SQL logging in hibernate.properties
                        logger.debug("ST_DWithin predicate added with parameters: " +
                                    "lon1={}, lat1={}, lon2={}, lat2={}, radius={}m", 
                                    institute.getLongitude(), institute.getLatitude(), 
                                    "property.longitude", "property.latitude", radiusInMeters);
                    }
                    
                    // Calculate distance for sorting if needed
                    final boolean sortByDistanceInner = sortByDistance;
                    if (sortByDistanceInner) {
                            Expression<?> propertyPointForSort = cb.function("ST_SetSRID", Object.class,
                                cb.function("ST_MakePoint", Object.class, 
                                    root.get("longitude"), 
                                    root.get("latitude")),
                                cb.literal(SRID_WGS84)
                        );
                            Expression<?> propertyGeographyForSort = cb.function("geography", Object.class, propertyPointForSort);

                        final Expression<?> instituteGeographyFinal = instituteGeographyTemp;
                        distanceExpressionTemp = cb.function("ST_Distance", Double.class,
                                    propertyGeographyForSort,
                                instituteGeographyFinal
                            );
                        }
                    } else {
                    logger.warn("Institute with ID: {} not found. No results will be returned for this filter.", 
                              criteria.getInstituteId());
                    predicates.add(cb.disjunction()); // This ensures no results are returned
                }
            }
            
            // Apply WHERE clause
            query.where(cb.and(predicates.toArray(new Predicate[0])));
            
            // Apply ORDER BY
            List<Order> orders = new ArrayList<>();
            
            // First add distance sort if requested
            final Expression<Double> distanceExpressionFinal = distanceExpressionTemp;
            final boolean sortByDistanceInnerOrd = sortByDistance; // Renamed from sortByDistanceInner for clarity
            if (sortByDistanceInnerOrd && distanceExpressionFinal != null) {
                orders.add(sortAscending ? cb.asc(distanceExpressionFinal) : cb.desc(distanceExpressionFinal));
                logger.debug("Applied sort by distance: {}", distanceSortDirection);
            }
            
            // Then add other sorts from pageable
            if (pageableSortForLambdaInSearchWithDist.isSorted()) {
                for (Sort.Order sortOrder : pageableSortForLambdaInSearchWithDist) {
                    orders.add(sortOrder.isAscending() ? 
                              cb.asc(root.get(sortOrder.getProperty())) : 
                              cb.desc(root.get(sortOrder.getProperty())));
                }
            }
            
            // Apply a default sort if no other sorts
            if (orders.isEmpty()) {
                orders.add(cb.desc(root.get("listingDate")));
            }
            
            query.orderBy(orders);

            return query.getRestriction();
        };
        
        Page<PropertyListing> propertyPage = propertyListingRepository.findAll(spec, pageable);
        
        // Debug log properties found and their source types
        logger.debug("Query found {} properties", propertyPage.getNumberOfElements());
        Map<String, Long> sourceTypeCounts = propertyPage.getContent().stream()
            .collect(Collectors.groupingBy(
                p -> p.getSourceType() != null ? p.getSourceType().toString() : "NULL",
                Collectors.counting()
            ));
        logger.info("Property counts by source type: {}", sourceTypeCounts);

        propertyPage.getContent().forEach(property -> {
            logger.debug("Found property ID: {}, Title: {}, SourceType: {}, Lat: {}, Long: {}",
                    property.getId(), property.getTitle(), property.getSourceType(), 
                    property.getLatitude(), property.getLongitude());
        });
        
        Page<PropertyListingDTO> dtoPage = propertyPage.map(this::convertToDto);
        
        final boolean sortByDistanceFinal = sortByDistance;
        if (sortByDistanceFinal) {
            logger.debug("Sorted {} properties by distance {}", 
                       dtoPage.getNumberOfElements(), distanceSortDirection);
        }
        
        return dtoPage;
    }

    public Optional<PropertyListingDTO> getPropertyById(Long id) {
        logger.debug("Fetching property by ID: {}", id);
        return propertyListingRepository.findById(id)
                                      .map(this::convertToDto);
    }

    /**
     * Helper method to convert a PropertyListing entity to a PropertyListingDTO
     */
    private PropertyListingDTO convertToDto(PropertyListing propertyListing) {
        if (propertyListing == null) {
            return null;
        }
        
        logger.debug("Converting property ID: {} to DTO, source type: {}", 
                   propertyListing.getId(), propertyListing.getSourceType());
        
        PropertyListingDTO dto = PropertyListingDTO.builder()
                .id(propertyListing.getId())
                .title(propertyListing.getTitle())
                .description(propertyListing.getDescription())
                .price(propertyListing.getPrice())
                .location(propertyListing.getLocation())
                .area(propertyListing.getArea())
                .propertyType(propertyListing.getPropertyType())
                .contactInfo(propertyListing.getContactInfo())
                .city(propertyListing.getCity())
                .district(propertyListing.getDistrict())
                .fullAddress(propertyListing.getFullAddress())
                .latitude(propertyListing.getLatitude())
                .longitude(propertyListing.getLongitude())
                .formattedAddress(propertyListing.getFormattedAddress())
                .rooms(propertyListing.getRooms())
                .bedrooms(propertyListing.getBedrooms())
                .bathrooms(propertyListing.getBathrooms())
                .imageUrls(propertyListing.getImageUrls())
                .sourceUrl(propertyListing.getSourceUrl())
                .sourceWebsite(propertyListing.getSourceWebsite())
                .createdAt(propertyListing.getCreatedAt())
                .updatedAt(propertyListing.getUpdatedAt())
                .listingDate(propertyListing.getListingDate())
                .active(propertyListing.isActive())
                .build();
        
        // Set owner-specific fields
        dto.setSecurityDeposit(propertyListing.getSecurityDeposit());
        dto.setAvailableTo(propertyListing.getAvailableTo());
        dto.setPaymentFrequency(propertyListing.getPaymentFrequency());
        dto.setMinimumStayMonths(propertyListing.getMinimumStayMonths());
        dto.setHasBalcony(propertyListing.getHasBalcony());
        dto.setFloor(propertyListing.getFloor());
        dto.setAmenities(propertyListing.getAmenities());
        
        // Set source type - IMPORTANT for filtering
        if (propertyListing.getSourceType() != null) {
            dto.setSourceType(propertyListing.getSourceType().toString());
        } else {
            dto.setSourceType("SCRAPED"); // Default fallback for older data
        }
        
        // Set owner ID if available
        if (propertyListing.getUser() != null) {
            dto.setOwnerId(Long.valueOf(propertyListing.getUser().getId()));
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
    
    @Transactional
    public PropertyListingDTO addImagesToProperty(Long propertyId, List<MultipartFile> imageFiles, User currentUser) {
        logger.info("User {} attempting to add images to property ID: {}", currentUser.getUsername(), propertyId);
        PropertyListing propertyListing = propertyListingRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("PropertyListing not found with id: " + propertyId));

        // Authorization: Check if the current user is the owner or an admin
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        if (propertyListing.getUser() == null || (propertyListing.getUser().getId() != currentUser.getId() && !isAdmin)) {
            logger.warn("Access denied for User {} to add images to property ID: {}. User is not owner or admin.", currentUser.getUsername(), propertyId);
            throw new AccessDeniedException("You are not authorized to add images to this property.");
        }

        // Logic to save files (e.g., to a directory or cloud storage) and get their URLs
        // This is a placeholder. You'll need to implement actual file storage.
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile imageFile : imageFiles) {
            if (!imageFile.isEmpty()) {
                // Simulate saving the file and getting a URL
                // In a real application, you would use a file storage service
                String fileName = StringUtils.cleanPath(Objects.requireNonNull(imageFile.getOriginalFilename()));
                String imageUrl = "/uploads/" + propertyId + "/" + fileName; // Example URL structure
                // TODO: Implement actual file saving logic here
                // e.g., Files.copy(imageFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                imageUrls.add(imageUrl);
                logger.info("Simulated saving image {} for property ID {}", fileName, propertyId);
            }
        }

        // Add new URLs to existing ones (if any)
        if (propertyListing.getImageUrls() == null) {
            propertyListing.setImageUrls(new ArrayList<>());
        }
        propertyListing.getImageUrls().addAll(imageUrls);

        PropertyListing updatedProperty = propertyListingRepository.save(propertyListing);
        logger.info("Images added successfully to property ID: {} by User: {}", propertyId, currentUser.getUsername());
        return convertToDto(updatedProperty);
    }

    // New method to get properties by owner ID from the main listings table
    public List<PropertyListingDTO> getPropertiesByOwnerId(Integer ownerId) {
        logger.debug("Fetching properties for owner ID: {} from main listings", ownerId);
        List<PropertyListing> listings = propertyListingRepository.findByUserId(ownerId);
        return listings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PropertyListingDTO createPropertyByOwner(PropertyListingCreateDTO dto, Authentication authentication) {
        // Extract email from JWT token or Authentication
        String email = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getClaim("email");
        } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            email = authentication.getName(); // Fallback
            logger.warn("Using fallback authentication.getName() in createPropertyByOwner: {}", email);
        }

        // Find user by email
        User owner = userRepository.findByEmail(email);
        if (owner == null) {
            // Try alternative lookup if needed
            if (email != null && !email.contains("@")) {
                String alternativeEmail = email + "@gmail.com"; // Default domain fallback
                logger.info("User not found by email '{}', trying alternative: {}", email, alternativeEmail);
                owner = userRepository.findByEmail(alternativeEmail);
            }
            if (owner == null) {
                logger.error("User not found for email/username: {} during property creation", email);
                throw new ResourceNotFoundException("User not found for email/username: " + email);
            }
        }

        // Use modelMapper instead of direct getter calls
        PropertyListing property = modelMapper.map(dto, PropertyListing.class);
        
        // Set source type to OWNER
        property.setSourceType(PropertyListing.SourceType.OWNER);
        
        // Set owner
        property.setUser(owner);
        
        // Set dates
        property.setListingDate(LocalDateTime.now());
        property.setActive(true);

        // Set geocoding information if needed
        if (property.getFullAddress() != null && !property.getFullAddress().trim().isEmpty()) {
            try {
                double[] coordinates = geocodingService.geocode(property.getFullAddress());
                property.setLatitude(coordinates[0]);
                property.setLongitude(coordinates[1]);
                property.setFormattedAddress(property.getFullAddress());
            } catch (Exception e) {
                // Handle geocoding error
                property.setLatitude(0.0);
                property.setLongitude(0.0);
                property.setFormattedAddress(property.getFullAddress());
            }
        }

        // Save the property listing
        PropertyListing savedProperty = propertyListingRepository.save(property);
        
        return convertToDto(savedProperty);
    }

    @Transactional
    public PropertyListingDTO addImagesToProperty(Long propertyId, List<MultipartFile> images, Authentication authentication) {
        PropertyListing property = propertyListingRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));
        
        // Extract email from JWT token or Authentication
        String email = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getClaim("email");
        } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            email = authentication.getName(); // Fallback
            logger.warn("Using fallback authentication.getName() in addImagesToProperty: {}", email);
        }

        // Find user by email
        User owner = userRepository.findByEmail(email);
        if (owner == null) {
            // Try alternative lookup if needed
            if (email != null && !email.contains("@")) {
                String alternativeEmail = email + "@gmail.com"; // Default domain fallback
                logger.info("User not found by email '{}', trying alternative: {}", email, alternativeEmail);
                owner = userRepository.findByEmail(alternativeEmail);
            }
            if (owner == null) {
                logger.error("User not found for email/username: {} while adding images", email);
                throw new ResourceNotFoundException("User not found for email/username: " + email);
            }
        }
        
        if (property.getUser().getId() != owner.getId()) {
            throw new UnauthorizedAccessException("You do not have permission to modify this property");
        }
        
        List<String> imageUrls = new ArrayList<>();
        
        // Create directory if it doesn't exist
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // Process each image
        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                try {
                    // Generate unique filename
                    String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                    Path filePath = Paths.get(UPLOAD_DIR + filename);
                    
                    // Save file
                    Files.write(filePath, image.getBytes());
                    
                    // Add URL to list
                    String imageUrl = "/api/images/" + filename;
                    imageUrls.add(imageUrl);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to store image", e);
                }
            }
        }
        
        // Update property with new images
        if (!imageUrls.isEmpty()) {
            if (property.getImageUrls() == null) {
                property.setImageUrls(new ArrayList<>());
            }
            property.getImageUrls().addAll(imageUrls);
            propertyListingRepository.save(property);
        }
        
        return convertToDto(property);
    }

    @Transactional
    public PropertyListingDTO updatePropertyByOwner(Long propertyId, PropertyListingCreateDTO dto, Authentication authentication) {
        PropertyListing property = propertyListingRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));
        
        // Extract email from JWT token or Authentication
        String email = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getClaim("email");
        } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            email = authentication.getName(); // Fallback
            logger.warn("Using fallback authentication.getName() in updatePropertyByOwner: {}", email);
        }

        // Find user by email
        User owner = userRepository.findByEmail(email);
        if (owner == null) {
            // Try alternative lookup if needed
            if (email != null && !email.contains("@")) {
                String alternativeEmail = email + "@gmail.com"; // Default domain fallback
                logger.info("User not found by email '{}', trying alternative: {}", email, alternativeEmail);
                owner = userRepository.findByEmail(alternativeEmail);
            }
            if (owner == null) {
                logger.error("User not found for email/username: {} during property update", email);
                throw new ResourceNotFoundException("User not found for email/username: " + email);
            }
        }
        
        if (property.getUser().getId() != owner.getId()) {
            throw new UnauthorizedAccessException("You do not have permission to modify this property");
        }
        
        // Save original values we want to preserve
        User originalOwner = property.getUser();
        LocalDateTime originalListingDate = property.getListingDate();
        boolean originalActive = property.isActive();
        
        // Use modelMapper to map non-null properties
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(dto, property);
        modelMapper.getConfiguration().setSkipNullEnabled(false);
        
        // Restore preserved values
        property.setUser(originalOwner);
        property.setListingDate(originalListingDate);
        property.setActive(originalActive);
        
        // Re-geocode if address changed
        if (property.getFullAddress() != null && !property.getFullAddress().trim().isEmpty()) {
            try {
                double[] coordinates = geocodingService.geocode(property.getFullAddress());
                property.setLatitude(coordinates[0]);
                property.setLongitude(coordinates[1]);
                property.setFormattedAddress(property.getFullAddress());
            } catch (Exception e) {
                // Handle geocoding error - keep existing coordinates if available
                logger.warn("Geocoding failed for address: {}, keeping existing coordinates", property.getFullAddress());
            }
        }
        
        // Save the updated property
        PropertyListing updatedProperty = propertyListingRepository.save(property);
        
        return convertToDto(updatedProperty);
    }

    @Transactional
    public void deletePropertyByOwner(Long propertyId, Authentication authentication) {
        PropertyListing property = propertyListingRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));
        
        // Verify ownership
        String email = null;
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getClaim("email");
        } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            email = authentication.getName(); // Fallback, but log a warning if this happens
            logger.warn("Using fallback authentication.getName() in deletePropertyByOwner: {}", email);
        }

        User owner = userRepository.findByEmail(email);
        if (owner == null) {
            // Attempt fallback to username if email (which might be a username) doesn't work
            if (email != null && !email.contains("@")) {
                String alternativeEmail = email + "@gmail.com"; // Or your default domain
                logger.info("User not found by email '{}', trying alternative: {}", email, alternativeEmail);
                owner = userRepository.findByEmail(alternativeEmail);
            }
            if (owner == null) {
                 logger.error("User not found for email/username: {} during delete operation", email);
                throw new ResourceNotFoundException("User not found for email/username: " + email);
            }
        }
        
        if (property.getUser().getId() != owner.getId()) {
            throw new UnauthorizedAccessException("You do not have permission to delete this property");
        }
        
        // Delete property
        propertyListingRepository.delete(property);
        logger.info("Property ID: {} deleted successfully by User ID: {}", propertyId, owner.getId());
    }

    // Get all properties belonging to the currently authenticated owner
    public List<PropertyListingDTO> getPropertiesByOwner(Authentication authentication) {
        // Add defensive handling for when authentication is null
        if (authentication == null) {
            logger.warn("Authentication is null in getPropertiesByOwner, returning empty list");
            return new ArrayList<>(); // Return empty list rather than throwing exception
        }
        
        try {
            // Fix: Extract email from JWT token claims instead of using authentication.getName()
            String email = null;
            if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
                // If using JWT token
                org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
                email = jwt.getClaim("email");
            } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                // If using UserDetails
                email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
            } else {
                // Fallback to getName, but log warning
                email = authentication.getName();
                logger.warn("Using fallback authentication.getName(): {}", email);
            }
            
            logger.info("Looking up user with email: {}", email);
            User owner = userRepository.findByEmail(email);
            
            if (owner == null) {
                logger.warn("User not found for email: {}", email);
                
                // Try alternative lookup by username if email fails
                if (email.indexOf('@') == -1) {
                    logger.info("Email appears to be username, trying to find user with username as email prefix");
                    // Try with @gmail.com suffix as fallback
                    String alternativeEmail = email + "@gmail.com";
                    logger.info("Trying alternative email: {}", alternativeEmail);
                    owner = userRepository.findByEmail(alternativeEmail);
                    
                    if (owner == null) {
                        logger.warn("User not found with alternative email: {}", alternativeEmail);
                        return new ArrayList<>();
                    }
                } else {
                    return new ArrayList<>();
                }
            }
            
            List<PropertyListing> properties = propertyListingRepository.findByUser(owner);
            logger.info("Found {} properties for user ID: {}", properties.size(), owner.getId());
            
            return properties.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error in getPropertiesByOwner: ", e);
            return new ArrayList<>(); // Return empty list on any exception
        }
    }
    
    /**
     * Gets the latest properties by source type, sorted by listing date (most recent first)
     * 
     * @param sourceType The source type (e.g. "OWNER", "SCRAPED")
     * @param limit The maximum number of properties to return
     * @return List of property DTOs sorted by listing date descending
     */
    public List<PropertyListingDTO> getLatestPropertiesBySourceType(String sourceType, int limit) {
        logger.info("Fetching latest {} properties with source type: {}", limit, sourceType);
        
        PropertyListing.SourceType enumSourceType;
        try {
            enumSourceType = PropertyListing.SourceType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid source type: {}", sourceType, e);
            return new ArrayList<>();
        }
        
        // Create a pageable to limit and sort results
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit, 
                Sort.by(Sort.Direction.DESC, "listingDate"));
        
        // Get latest active properties of the specified source type
        Page<PropertyListing> propertyPage = propertyListingRepository.findBySourceTypeAndActiveTrue(enumSourceType, pageable);
        
        return propertyPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the properties for a specific city, limited by count.
     * This method handles case insensitivity for the city name.
     * 
     * @param city The name of the city to filter by
     * @param limit Maximum number of properties to return
     * @return A list of PropertyListingDTO representing properties in the specified city
     */
    public List<PropertyListingDTO> getPropertiesByCity(String city, int limit) {
        logger.info("Fetching up to {} properties for city: {}", limit, city);
        
        if (city == null || city.trim().isEmpty()) {
            logger.warn("City parameter is null or empty");
            return new ArrayList<>();
        }
        
        // Create a pageable to limit and sort results
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit, 
                Sort.by(Sort.Direction.DESC, "listingDate"));
        
        // Get properties for the specified city (case insensitive)
        // Try both exact city match and location containing city name for better results
        List<PropertyListing> properties = propertyListingRepository
                .findByCityOrLocationContainingIgnoreCaseAndActiveTrue(city.trim(), pageable);
        
        logger.info("Found {} properties for city: {}", properties.size(), city);
        
        return properties.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Helper method to convert PropertyListing to PropertyListingDTO
    private PropertyListingDTO convertToDTO(PropertyListing property) {
        return convertToDto(property);
    }

    // Get all latest properties for a specific user
    public List<PropertyListingDTO> getUserLatestProperties(String username, int limit) {
        logger.info("Fetching latest {} properties for user: {}", limit, username);
        
        // Find the user by username
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            logger.warn("User not found with username: {}", username);
            return new ArrayList<>();
        }
        
        // Create a pageable object with sorting by listing date desc and limit
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(
                0, 
                limit, 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "listingDate")
            );
        
        // Find active properties by user, sorted
        List<PropertyListing> properties = propertyListingRepository.findByUserAndActiveTrue(user, pageable);
        
        logger.info("Found {} properties for user: {}", properties.size(), username);
        
        return properties.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Archive a property listing (mark as inactive)
     * @param propertyId the ID of the property to archive
     * @param currentUser the authenticated user principal
     * @return the updated property listing DTO
     */
    @Transactional
    public PropertyListingDTO archiveProperty(Long propertyId, UserPrincipal currentUser) {
        logger.info("Archiving property {} by user {}", propertyId, currentUser.getId());
        
        // Fetch the property
        PropertyListing property = propertyListingRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));
        
        // Authorize: Ensure currentUser is the owner of the property
        if (property.getUser() == null || property.getUser().getId() != currentUser.getId()) {
            throw new AccessDeniedException("You don't have permission to archive this property");
        }
        
        // Archive the property by setting active to false
        property.setActive(false);
        
        // Save the updated property
        property = propertyListingRepository.save(property);
        
        logger.info("Property {} archived successfully", propertyId);
        
        return convertToDTO(property);
    }
} 