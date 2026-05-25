// File: src/main/java/com/eserian/homes/controller/AdminController.java

package com.eserian.homes.controller;

import com.eserian.homes.entity.Admin;
import com.eserian.homes.model.Property;
import com.eserian.homes.model.User;
import com.eserian.homes.model.PropertyPhoto;
import com.eserian.homes.model.Video;
import com.eserian.homes.repository.PropertyRepository;
import com.eserian.homes.repository.UserRepository;
import com.eserian.homes.repository.PropertyPhotoRepository;
import com.eserian.homes.repository.VideoRepository;
import com.eserian.homes.service.AdminService;
import com.eserian.homes.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyPhotoRepository propertyPhotoRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    // ============================================
    // DASHBOARD METHODS
    // ============================================

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<?> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalProperties = propertyRepository.count();
        long pendingProperties = propertyRepository.findByStatus("PENDING").size();
        long approvedProperties = propertyRepository.findByStatus("APPROVED").size();

        double monthlyRevenue = 0.0;

        metrics.put("total_users", totalUsers);
        metrics.put("total_properties", totalProperties);
        metrics.put("pending_properties", pendingProperties);
        metrics.put("approved_properties", approvedProperties);
        metrics.put("monthly_revenue", monthlyRevenue);
        metrics.put("user_growth", 0);
        metrics.put("booking_growth", 0);
        metrics.put("revenue_growth", 0);
        metrics.put("occupancy_rate", 0);
        metrics.put("cancellation_rate", 0);

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/pending/counts")
    public ResponseEntity<?> getPendingCounts() {
        Map<String, Object> counts = new HashMap<>();
        counts.put("properties", propertyRepository.findByStatus("PENDING").size());
        counts.put("kyc", 0);
        counts.put("disputes", 0);
        counts.put("payouts", 0);
        return ResponseEntity.ok(counts);
    }

    // ============================================
    // PROPERTY MANAGEMENT - INCLUDES PHOTOS AND VIDEOS
    // ============================================

    @GetMapping("/properties/pending")
    public ResponseEntity<?> getPendingProperties() {
        try {
            List<Property> pendingProperties = propertyRepository.findByStatus("PENDING");

            List<Map<String, Object>> result = new ArrayList<>();
            for (Property property : pendingProperties) {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", property.getId());
                dto.put("title", property.getTitle());
                dto.put("description", property.getDescription() != null ? property.getDescription() : "");
                dto.put("location", property.getLocation() != null ? property.getLocation() : "");
                dto.put("pricePerNight", property.getPricePerNight() != null ? property.getPricePerNight() : 0);
                dto.put("propertyType", property.getPropertyType() != null ? property.getPropertyType() : "N/A");
                dto.put("status", property.getStatus());
                dto.put("risk_score", property.getRiskScore() != null ? property.getRiskScore() : 0);
                dto.put("createdAt", property.getCreatedAt());
                dto.put("bedrooms", property.getBedrooms() != null ? property.getBedrooms() : 1);
                dto.put("bathrooms", property.getBathrooms() != null ? property.getBathrooms() : 1);
                dto.put("amenities", property.getAmenities() != null ? property.getAmenities() : "");

                // Get owner name
                Optional<User> ownerOpt = userRepository.findById(property.getOwnerId());
                dto.put("owner_name", ownerOpt.isPresent() ? ownerOpt.get().getName() : "Unknown");
                dto.put("owner_id", property.getOwnerId());

                // ========== LOAD PHOTOS ==========
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

                // ========== LOAD VIDEOS ==========
                List<Video> videos = videoRepository.findByPropertyIdOrderByOrderIndexAsc(property.getId());
                List<Map<String, Object>> videoList = new ArrayList<>();
                for (Video video : videos) {
                    Map<String, Object> videoMap = new HashMap<>();
                    videoMap.put("id", video.getId());
                    videoMap.put("videoPath", video.getVideoPath());
                    videoMap.put("title", video.getTitle() != null ? video.getTitle() : "");
                    videoMap.put("description", video.getDescription() != null ? video.getDescription() : "");
                    videoMap.put("isFeatured", video.getIsFeatured());
                    videoMap.put("orderIndex", video.getOrderIndex());
                    videoList.add(videoMap);
                }
                dto.put("videos", videoList);
                dto.put("videoCount", videoList.size());

                result.add(dto);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch pending properties: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/properties/{id}")
    public ResponseEntity<?> getPropertyById(@PathVariable Long id) {
        try {
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (!propertyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Property property = propertyOpt.get();
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", property.getId());
            dto.put("title", property.getTitle());
            dto.put("description", property.getDescription());
            dto.put("location", property.getLocation());
            dto.put("pricePerNight", property.getPricePerNight());
            dto.put("propertyType", property.getPropertyType());
            dto.put("status", property.getStatus());
            dto.put("risk_score", property.getRiskScore() != null ? property.getRiskScore() : 0);
            dto.put("createdAt", property.getCreatedAt());
            dto.put("updatedAt", property.getUpdatedAt());
            dto.put("bedrooms", property.getBedrooms());
            dto.put("bathrooms", property.getBathrooms());
            dto.put("amenities", property.getAmenities());
            dto.put("owner_id", property.getOwnerId());

            // Get owner name
            Optional<User> ownerOpt = userRepository.findById(property.getOwnerId());
            dto.put("owner_name", ownerOpt.isPresent() ? ownerOpt.get().getName() : "Unknown");
            dto.put("owner_email", ownerOpt.isPresent() ? ownerOpt.get().getEmail() : "Unknown");

            // Load photos
            List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderByOrderIndexAsc(id);
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

            // Load videos
            List<Video> videos = videoRepository.findByPropertyIdOrderByOrderIndexAsc(id);
            List<Map<String, Object>> videoList = new ArrayList<>();
            for (Video video : videos) {
                Map<String, Object> videoMap = new HashMap<>();
                videoMap.put("id", video.getId());
                videoMap.put("videoPath", video.getVideoPath());
                videoMap.put("title", video.getTitle());
                videoMap.put("description", video.getDescription());
                videoMap.put("isFeatured", video.getIsFeatured());
                videoMap.put("orderIndex", video.getOrderIndex());
                videoList.add(videoMap);
            }
            dto.put("videos", videoList);
            dto.put("videoCount", videoList.size());

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch property: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/properties/{id}/approve")
    public ResponseEntity<?> approveProperty(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (!propertyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Property property = propertyOpt.get();
            property.setStatus("APPROVED");
            property.setUpdatedAt(LocalDateTime.now());
            propertyRepository.save(property);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property approved successfully");
            response.put("property_id", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("message", "Failed to approve property: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/properties/{id}/reject")
    public ResponseEntity<?> rejectProperty(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (!propertyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Property property = propertyOpt.get();
            property.setStatus("REJECTED");
            property.setUpdatedAt(LocalDateTime.now());
            propertyRepository.save(property);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property rejected");
            response.put("reason", request.getOrDefault("reason", "Not specified"));
            response.put("property_id", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("message", "Failed to reject property: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/properties/{id}/suspend")
    public ResponseEntity<?> suspendProperty(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (!propertyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Property property = propertyOpt.get();
            property.setAdminStatus("suspended");
            property.setUpdatedAt(LocalDateTime.now());
            propertyRepository.save(property);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property suspended");
            response.put("property_id", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("message", "Failed to suspend property: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/properties/{id}/permanent")
    public ResponseEntity<?> deletePropertyPermanently(@PathVariable Long id) {
        try {
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (!propertyOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Delete associated photos first
            propertyPhotoRepository.deleteByPropertyId(id);
            // Delete associated videos
            videoRepository.deleteByPropertyId(id);
            // Delete the property
            propertyRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property deleted permanently");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("message", "Failed to delete property: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ============================================
    // USER MANAGEMENT
    // ============================================

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", user.getId());
            dto.put("name", user.getName());
            dto.put("email", user.getEmail());
            dto.put("role", user.getRole());
            dto.put("created_at", user.getCreatedAt());
            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("name", user.getName());
        dto.put("email", user.getEmail());
        dto.put("role", user.getRole());
        dto.put("created_at", user.getCreatedAt());

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/activities")
    public ResponseEntity<?> getRecentActivities(@RequestParam(defaultValue = "10") int limit) {
        // Return empty list for now - implement as needed
        return ResponseEntity.ok(new ArrayList<>());
    }

    @GetMapping("/fraud/alerts")
    public ResponseEntity<?> getFraudAlerts(@RequestParam(defaultValue = "5") int limit) {
        // Return empty list for now - implement as needed
        return ResponseEntity.ok(new ArrayList<>());
    }
}