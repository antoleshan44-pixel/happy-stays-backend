package com.eserian.homes.controller;

import com.eserian.homes.model.Property;
import com.eserian.homes.model.PropertyPhoto;
import com.eserian.homes.model.Video;
import com.eserian.homes.service.PropertyService;
import com.eserian.homes.repository.PropertyPhotoRepository;
import com.eserian.homes.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.eserian.homes.util.JwtUtil;
import com.eserian.homes.model.User;
import com.eserian.homes.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyPhotoRepository propertyPhotoRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        User user = getCurrentUser(request);
        return user != null ? user.getId() : null;
    }

    // ============================================
    // OWNER-ONLY ENDPOINTS (Require authentication)
    // ============================================

    // CREATE property (for owners)
    @PostMapping
    public ResponseEntity<?> createProperty(@RequestBody Property property, HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            property.setOwnerId(currentUserId);
            property.setStatus("PENDING");
            property.setCreatedAt(LocalDateTime.now());
            property.setUpdatedAt(LocalDateTime.now());

            Property created = propertyService.createProperty(property);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating property: " + e.getMessage());
        }
    }

    // UPDATE property (for owners)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProperty(@PathVariable Long id, @RequestBody Property property, HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            Property existing = propertyService.getPropertyById(id);

            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            if (!existing.getOwnerId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't own this property");
            }

            existing.setTitle(property.getTitle());
            existing.setDescription(property.getDescription());
            existing.setLocation(property.getLocation());
            existing.setPricePerNight(property.getPricePerNight());
            existing.setBedrooms(property.getBedrooms());
            existing.setBathrooms(property.getBathrooms());
            existing.setImageUrl(property.getImageUrl());
            existing.setAmenities(property.getAmenities());
            existing.setPropertyType(property.getPropertyType());
            existing.setUpdatedAt(LocalDateTime.now());

            Property updated = propertyService.updateProperty(id, existing);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating property: " + e.getMessage());
        }
    }

    // DELETE property (for owners)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProperty(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            Property existing = propertyService.getPropertyById(id);

            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            if (!existing.getOwnerId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't own this property");
            }

            if (propertyService.deleteProperty(id)) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting property: " + e.getMessage());
        }
    }

    // GET properties for current owner (my-properties)
    @GetMapping("/my-properties")
    public ResponseEntity<?> getMyProperties(HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            List<Property> properties = propertyService.getPropertiesByOwnerId(currentUserId);

            // Convert to DTOs with photos
            List<Map<String, Object>> result = new ArrayList<>();
            for (Property property : properties) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", property.getId());
                dto.put("title", property.getTitle());
                dto.put("pricePerNight", property.getPricePerNight());
                dto.put("location", property.getLocation());
                dto.put("status", property.getStatus());

                // Load photos count
                int photoCount = propertyPhotoRepository.countByPropertyId(property.getId());
                dto.put("photoCount", photoCount);

                result.add(dto);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching properties: " + e.getMessage());
        }
    }
}