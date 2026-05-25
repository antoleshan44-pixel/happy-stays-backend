package com.eserian.homes.controller;

import com.eserian.homes.model.Property;
import com.eserian.homes.model.PropertyPhoto;
import com.eserian.homes.model.User;
import com.eserian.homes.model.Video;
import com.eserian.homes.repository.PropertyPhotoRepository;
import com.eserian.homes.repository.PropertyRepository;
import com.eserian.homes.repository.UserRepository;
import com.eserian.homes.repository.VideoRepository;
import com.eserian.homes.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyPhotoRepository propertyPhotoRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // ============================================
    // PUBLIC PROPERTY ENDPOINTS (No authentication required)
    // ============================================

    /**
     * GET all approved properties (public)
     * Returns properties with their photos
     */
    @GetMapping("/properties/approved")
    public ResponseEntity<List<Map<String, Object>>> getApprovedProperties() {
        List<Property> properties = propertyRepository.findByStatus("APPROVED");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Property property : properties) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", property.getId());
            dto.put("title", property.getTitle());
            dto.put("description", property.getDescription());
            dto.put("pricePerNight", property.getPricePerNight());
            dto.put("location", property.getLocation());
            dto.put("propertyType", property.getPropertyType());
            dto.put("averageRating", 4.5);
            dto.put("createdAt", property.getCreatedAt());

            // Load photos
            List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderByOrderIndexAsc(property.getId());
            List<Map<String, Object>> photoList = new ArrayList<>();
            for (PropertyPhoto photo : photos) {
                Map<String, Object> photoMap = new HashMap<>();
                photoMap.put("id", photo.getId());
                photoMap.put("photoPath", photo.getPhotoPath());
                photoMap.put("isPrimary", photo.getIsPrimary());
                photoMap.put("orderIndex", photo.getOrderIndex());
                photoList.add(photoMap);
            }
            dto.put("photos", photoList);
            dto.put("photoCount", photoList.size());

            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET property by ID with photos and videos (public)
     */
    @GetMapping("/properties/{id}")
    public ResponseEntity<Map<String, Object>> getPropertyById(@PathVariable Long id) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);

        if (!propertyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Property property = propertyOpt.get();
        Map<String, Object> dto = new HashMap<>();

        // Basic property information
        dto.put("id", property.getId());
        dto.put("title", property.getTitle());
        dto.put("description", property.getDescription());
        dto.put("pricePerNight", property.getPricePerNight());
        dto.put("location", property.getLocation());
        dto.put("propertyType", property.getPropertyType());
        dto.put("status", property.getStatus());
        dto.put("createdAt", property.getCreatedAt());
        dto.put("updatedAt", property.getUpdatedAt());
        dto.put("bedrooms", property.getBedrooms());
        dto.put("bathrooms", property.getBathrooms());
        dto.put("amenities", property.getAmenities());
        dto.put("ownerId", property.getOwnerId());
        dto.put("averageRating", 4.5);
        dto.put("risk_score", property.getRiskScore() != null ? property.getRiskScore() : 0);

        // Get owner name
        Optional<User> ownerOpt = userRepository.findById(property.getOwnerId());
        dto.put("owner_name", ownerOpt.isPresent() ? ownerOpt.get().getName() : "Eserian Host");
        dto.put("owner_email", ownerOpt.isPresent() ? ownerOpt.get().getEmail() : "");

        // ========== LOAD PHOTOS ==========
        List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderByOrderIndexAsc(property.getId());
        List<Map<String, Object>> photoList = new ArrayList<>();
        for (PropertyPhoto photo : photos) {
            Map<String, Object> photoMap = new HashMap<>();
            photoMap.put("id", photo.getId());
            photoMap.put("photoPath", photo.getPhotoPath());
            photoMap.put("isPrimary", photo.getIsPrimary() != null ? photo.getIsPrimary() : false);
            photoMap.put("orderIndex", photo.getOrderIndex() != null ? photo.getOrderIndex() : 0);
            photoList.add(photoMap);
        }
        dto.put("photos", photoList);
        dto.put("photoCount", photoList.size());

        // Set main photo URL
        if (!photoList.isEmpty()) {
            String mainPhotoUrl = photoList.get(0).get("photoPath").toString();
            for (Map<String, Object> photo : photoList) {
                if (Boolean.TRUE.equals(photo.get("isPrimary"))) {
                    mainPhotoUrl = photo.get("photoPath").toString();
                    break;
                }
            }
            dto.put("mainPhotoUrl", mainPhotoUrl);
        } else {
            dto.put("mainPhotoUrl", null);
        }

        // ========== LOAD VIDEOS ==========
        List<Video> videos = videoRepository.findByPropertyIdOrderByOrderIndexAsc(property.getId());
        List<Map<String, Object>> videoList = new ArrayList<>();
        for (Video video : videos) {
            Map<String, Object> videoMap = new HashMap<>();
            videoMap.put("id", video.getId());
            videoMap.put("videoPath", video.getVideoPath());
            videoMap.put("title", video.getTitle() != null ? video.getTitle() : "");
            videoMap.put("description", video.getDescription() != null ? video.getDescription() : "");
            videoMap.put("isFeatured", video.getIsFeatured() != null ? video.getIsFeatured() : false);
            videoMap.put("orderIndex", video.getOrderIndex() != null ? video.getOrderIndex() : 0);
            videoList.add(videoMap);
        }
        dto.put("videos", videoList);
        dto.put("videoCount", videoList.size());

        // Set featured video URL
        if (!videoList.isEmpty()) {
            String featuredVideoUrl = videoList.get(0).get("videoPath").toString();
            for (Map<String, Object> video : videoList) {
                if (Boolean.TRUE.equals(video.get("isFeatured"))) {
                    featuredVideoUrl = video.get("videoPath").toString();
                    break;
                }
            }
            dto.put("featuredVideoUrl", featuredVideoUrl);
        } else {
            dto.put("featuredVideoUrl", null);
        }

        // Increment view count
        property.setViewCount((property.getViewCount() != null ? property.getViewCount() : 0) + 1);
        propertyRepository.save(property);

        return ResponseEntity.ok(dto);
    }

    /**
     * Search properties by keyword and filters (public)
     */
    @GetMapping("/properties/search")
    public ResponseEntity<List<Map<String, Object>>> searchProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String propertyType) {

        List<Property> properties = propertyRepository.findByStatus("APPROVED");

        // Apply filters
        if (search != null && !search.isEmpty()) {
            properties = properties.stream()
                    .filter(p -> p.getTitle().toLowerCase().contains(search.toLowerCase()) ||
                            p.getDescription().toLowerCase().contains(search.toLowerCase()))
                    .toList();
        }

        if (location != null && !location.isEmpty()) {
            properties = properties.stream()
                    .filter(p -> p.getLocation() != null && p.getLocation().toLowerCase().contains(location.toLowerCase()))
                    .toList();
        }

        if (minPrice != null) {
            properties = properties.stream()
                    .filter(p -> p.getPricePerNight() >= minPrice)
                    .toList();
        }

        if (maxPrice != null) {
            properties = properties.stream()
                    .filter(p -> p.getPricePerNight() <= maxPrice)
                    .toList();
        }

        if (propertyType != null && !propertyType.isEmpty()) {
            properties = properties.stream()
                    .filter(p -> p.getPropertyType() != null && p.getPropertyType().equalsIgnoreCase(propertyType))
                    .toList();
        }

        // Convert to DTOs with photos
        List<Map<String, Object>> result = new ArrayList<>();
        for (Property property : properties) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", property.getId());
            dto.put("title", property.getTitle());
            dto.put("pricePerNight", property.getPricePerNight());
            dto.put("location", property.getLocation());
            dto.put("propertyType", property.getPropertyType());
            dto.put("averageRating", 4.5);

            // Load first photo only for search results
            List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderByOrderIndexAsc(property.getId());
            if (!photos.isEmpty()) {
                dto.put("mainPhotoUrl", photos.get(0).getPhotoPath());
            } else {
                dto.put("mainPhotoUrl", null);
            }
            dto.put("photoCount", photos.size());

            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }
}