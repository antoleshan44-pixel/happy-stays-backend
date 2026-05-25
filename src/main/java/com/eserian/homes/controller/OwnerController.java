// src/main/java/com/eserian/homes/controller/OwnerController.java

package com.eserian.homes.controller;

import com.eserian.homes.dto.PhotoDTO;
import com.eserian.homes.dto.PropertyDTO;
import com.eserian.homes.dto.VideoDTO;
import com.eserian.homes.model.*;
import com.eserian.homes.repository.*;
import com.eserian.homes.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyPhotoRepository propertyPhotoRepository;

    @Autowired
    private BlockedDateRepository blockedDateRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    private User getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    // ============================================
    // DASHBOARD
    // ============================================

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        List<Property> myProperties = propertyRepository.findByOwnerId(currentUser.getId());

        long totalProperties = myProperties.size();
        long approvedProperties = myProperties.stream().filter(p -> "APPROVED".equals(p.getStatus())).count();
        long pendingProperties = myProperties.stream().filter(p -> "PENDING".equals(p.getStatus())).count();

        long totalBookings = 0;
        double totalEarnings = 0;

        for (Property property : myProperties) {
            List<Booking> propertyBookings = bookingRepository.findByPropertyId(property.getId());
            totalBookings += propertyBookings.size();
            totalEarnings += propertyBookings.stream()
                    .filter(b -> "COMPLETED".equals(b.getStatus()))
                    .mapToDouble(Booking::getTotalPrice)
                    .sum();
        }

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalProperties", totalProperties);
        dashboard.put("approvedProperties", approvedProperties);
        dashboard.put("pendingProperties", pendingProperties);
        dashboard.put("totalBookings", totalBookings);
        dashboard.put("totalEarnings", totalEarnings);

        return ResponseEntity.ok(dashboard);
    }

    // ============================================
    // PROPERTY MANAGEMENT - FIXED: Now includes photos and videos
    // ============================================

    @GetMapping("/properties")
    public ResponseEntity<?> getMyProperties(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        List<Property> properties = propertyRepository.findByOwnerId(currentUser.getId());

        // Convert to DTOs with photos and videos included
        List<Map<String, Object>> propertyDTOs = new ArrayList<>();
        for (Property property : properties) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", property.getId());
            dto.put("title", property.getTitle());
            dto.put("description", property.getDescription());
            dto.put("location", property.getLocation());
            dto.put("pricePerNight", property.getPricePerNight());
            dto.put("status", property.getStatus());
            dto.put("propertyType", property.getPropertyType());
            dto.put("createdAt", property.getCreatedAt());
            dto.put("bedrooms", property.getBedrooms());
            dto.put("bathrooms", property.getBathrooms());
            dto.put("amenities", property.getAmenities());
            dto.put("video_path", property.getVideoPath());
            dto.put("video_thumbnail", property.getVideoThumbnail());

            // Load photos for this property
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

            // Load videos for this property
            List<Video> videos = videoRepository.findByPropertyIdOrderByOrderIndexAsc(property.getId());
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

            propertyDTOs.add(dto);
        }

        return ResponseEntity.ok(propertyDTOs);
    }

    @GetMapping("/properties/{propertyId}")
    public ResponseEntity<?> getPropertyById(@PathVariable Long propertyId, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            return ResponseEntity.notFound().build();
        }

        if (!property.getOwnerId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You don't own this property"));
        }

        // Load photos and convert to DTOs
        List<PropertyPhoto> photos = propertyPhotoRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId);
        List<Map<String, Object>> photoDTOs = new ArrayList<>();
        for (PropertyPhoto photo : photos) {
            Map<String, Object> photoDTO = new HashMap<>();
            photoDTO.put("id", photo.getId());
            photoDTO.put("photoPath", photo.getPhotoPath());
            photoDTO.put("isPrimary", photo.getIsPrimary());
            photoDTO.put("orderIndex", photo.getOrderIndex());
            photoDTOs.add(photoDTO);
        }

        // Load videos and convert to DTOs
        List<Video> videos = videoRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId);
        List<Map<String, Object>> videoDTOs = new ArrayList<>();
        for (Video video : videos) {
            Map<String, Object> videoDTO = new HashMap<>();
            videoDTO.put("id", video.getId());
            videoDTO.put("videoPath", video.getVideoPath());
            videoDTO.put("title", video.getTitle());
            videoDTO.put("description", video.getDescription());
            videoDTO.put("isFeatured", video.getIsFeatured());
            videoDTO.put("orderIndex", video.getOrderIndex());
            videoDTOs.add(videoDTO);
        }

        // Build property DTO
        Map<String, Object> response = new HashMap<>();
        response.put("id", property.getId());
        response.put("title", property.getTitle());
        response.put("description", property.getDescription());
        response.put("location", property.getLocation());
        response.put("pricePerNight", property.getPricePerNight());
        response.put("bedrooms", property.getBedrooms());
        response.put("bathrooms", property.getBathrooms());
        response.put("status", property.getStatus());
        response.put("propertyType", property.getPropertyType());
        response.put("amenities", property.getAmenities());
        response.put("ownerId", property.getOwnerId());
        response.put("createdAt", property.getCreatedAt());
        response.put("updatedAt", property.getUpdatedAt());
        response.put("photoCount", photoDTOs.size());
        response.put("videoCount", videoDTOs.size());
        response.put("photos", photoDTOs);
        response.put("videos", videoDTOs);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/properties")
    public ResponseEntity<?> createProperty(@RequestBody Map<String, Object> propertyData, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            Property property = new Property();
            property.setOwnerId(currentUser.getId());
            property.setTitle((String) propertyData.get("title"));
            property.setDescription((String) propertyData.get("description"));
            property.setLocation((String) propertyData.get("location"));
            property.setPropertyType((String) propertyData.get("propertyType"));

            Object bedrooms = propertyData.get("bedrooms");
            property.setBedrooms(bedrooms != null ? ((Number) bedrooms).intValue() : 1);

            Object bathrooms = propertyData.get("bathrooms");
            property.setBathrooms(bathrooms != null ? ((Number) bathrooms).intValue() : 1);

            Object pricePerNight = propertyData.get("pricePerNight");
            property.setPricePerNight(pricePerNight != null ? ((Number) pricePerNight).doubleValue() : 0);

            Object amenities = propertyData.get("amenities");
            if (amenities != null) {
                if (amenities instanceof List) {
                    property.setAmenities(amenities.toString());
                } else {
                    property.setAmenities((String) amenities);
                }
            }

            property.setStatus("PENDING");
            property.setCreatedAt(LocalDateTime.now());
            property.setUpdatedAt(LocalDateTime.now());

            Property saved = propertyRepository.save(property);

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("title", saved.getTitle());
            response.put("status", saved.getStatus());
            response.put("message", "Property created successfully. Awaiting admin approval.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating property: " + e.getMessage()));
        }
    }

    @PutMapping("/properties/{propertyId}")
    public ResponseEntity<?> updateProperty(@PathVariable Long propertyId,
                                            @RequestBody Map<String, Object> propertyData,
                                            HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            return ResponseEntity.notFound().build();
        }

        if (!property.getOwnerId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You don't own this property"));
        }

        try {
            if (propertyData.containsKey("title")) property.setTitle((String) propertyData.get("title"));
            if (propertyData.containsKey("description")) property.setDescription((String) propertyData.get("description"));
            if (propertyData.containsKey("location")) property.setLocation((String) propertyData.get("location"));
            if (propertyData.containsKey("propertyType")) property.setPropertyType((String) propertyData.get("propertyType"));
            if (propertyData.containsKey("pricePerNight")) property.setPricePerNight(((Number) propertyData.get("pricePerNight")).doubleValue());
            if (propertyData.containsKey("bedrooms")) property.setBedrooms(((Number) propertyData.get("bedrooms")).intValue());
            if (propertyData.containsKey("bathrooms")) property.setBathrooms(((Number) propertyData.get("bathrooms")).intValue());

            property.setUpdatedAt(LocalDateTime.now());

            Property updated = propertyRepository.save(property);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Property updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating property: " + e.getMessage()));
        }
    }

    @DeleteMapping("/properties/{propertyId}")
    public ResponseEntity<?> deleteProperty(@PathVariable Long propertyId, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            return ResponseEntity.notFound().build();
        }

        if (!property.getOwnerId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You don't own this property"));
        }

        List<Booking> existingBookings = bookingRepository.findByPropertyId(propertyId);
        boolean hasConfirmedBookings = existingBookings.stream()
                .anyMatch(b -> "CONFIRMED".equals(b.getStatus()) || "PENDING".equals(b.getStatus()));

        if (hasConfirmedBookings) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete property with existing bookings"));
        }

        propertyRepository.deleteById(propertyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Property deleted successfully");

        return ResponseEntity.ok(response);
    }

    // ============================================
    // PHOTO MANAGEMENT - FIXED (returns DTOs)
    // ============================================

    @PostMapping("/properties/{propertyId}/photos")
    public ResponseEntity<?> uploadPhotos(@PathVariable Long propertyId,
                                          @RequestParam("photos") List<MultipartFile> photos,
                                          HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            return ResponseEntity.notFound().build();
        }

        if (!property.getOwnerId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You don't own this property"));
        }

        try {
            List<Map<String, Object>> uploadedPhotoDTOs = new ArrayList<>();
            Path uploadPath = Paths.get(uploadDir, "properties", String.valueOf(propertyId), "photos");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            int existingCount = propertyPhotoRepository.countByPropertyId(propertyId);

            for (int i = 0; i < photos.size(); i++) {
                MultipartFile photo = photos.get(i);
                String filename = System.currentTimeMillis() + "_" + i + "_" + photo.getOriginalFilename();
                Path filePath = uploadPath.resolve(filename);
                Files.write(filePath, photo.getBytes());

                PropertyPhoto propertyPhoto = new PropertyPhoto();
                propertyPhoto.setProperty(property);
                propertyPhoto.setPhotoPath("properties/" + propertyId + "/photos/" + filename);
                propertyPhoto.setIsPrimary(existingCount == 0 && i == 0);
                propertyPhoto.setOrderIndex(existingCount + i);
                propertyPhoto.setCreatedAt(LocalDateTime.now());

                PropertyPhoto saved = propertyPhotoRepository.save(propertyPhoto);

                // Return DTO instead of entity
                Map<String, Object> photoDTO = new HashMap<>();
                photoDTO.put("id", saved.getId());
                photoDTO.put("photoPath", saved.getPhotoPath());
                photoDTO.put("isPrimary", saved.getIsPrimary());
                photoDTO.put("orderIndex", saved.getOrderIndex());
                uploadedPhotoDTOs.add(photoDTO);
            }

            // Return just the array of photos, not wrapped
            return ResponseEntity.ok(uploadedPhotoDTOs);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error uploading photos: " + e.getMessage()));
        }
    }

    // ============================================
    // VIDEO MANAGEMENT
    // ============================================

    @PostMapping("/properties/{propertyId}/videos")
    public ResponseEntity<?> uploadVideo(@PathVariable Long propertyId,
                                         @RequestParam("video") MultipartFile video,
                                         @RequestParam(value = "title", required = false) String title,
                                         @RequestParam(value = "description", required = false) String description,
                                         HttpServletRequest request) {

        System.out.println("=== VIDEO UPLOAD REQUEST ===");
        System.out.println("Property ID: " + propertyId);
        System.out.println("File name: " + (video != null ? video.getOriginalFilename() : "null"));
        System.out.println("File size: " + (video != null ? video.getSize() : 0));
        System.out.println("Title: " + title);

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            return ResponseEntity.notFound().build();
        }

        if (!property.getOwnerId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You don't own this property"));
        }

        if (video == null || video.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Video file is required"));
        }

        try {
            int existingCount = videoRepository.countByPropertyId(propertyId);
            System.out.println("Existing videos count: " + existingCount);

            if (existingCount >= 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Maximum 5 videos allowed per property"));
            }

            String uploadDirPath = uploadDir + "/properties/" + propertyId + "/videos";
            Path uploadPath = Paths.get(uploadDirPath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created directory: " + uploadDirPath);
            }

            String originalFilename = video.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = System.currentTimeMillis() + "_" + propertyId + fileExtension;
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, video.getBytes());

            System.out.println("Saved video to: " + filePath.toString());

            Video videoEntity = new Video();
            videoEntity.setProperty(property);
            videoEntity.setVideoPath("properties/" + propertyId + "/videos/" + filename);
            videoEntity.setTitle(title);
            videoEntity.setDescription(description);
            videoEntity.setIsFeatured(existingCount == 0);
            videoEntity.setOrderIndex(existingCount);
            videoEntity.setCreatedAt(LocalDateTime.now());

            Video saved = videoRepository.save(videoEntity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Video uploaded successfully");
            response.put("id", saved.getId());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("Error saving video: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error uploading video: " + e.getMessage()));
        }
    }

    @DeleteMapping("/properties/{propertyId}/videos/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long propertyId,
                                         @PathVariable Long videoId,
                                         HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null || !property.getOwnerId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }

        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || !video.getProperty().getId().equals(propertyId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            String filePath = uploadDir + "/" + video.getVideoPath();
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                System.out.println("Deleted video file: " + filePath);
            }
        } catch (IOException e) {
            System.err.println("Could not delete video file: " + e.getMessage());
        }

        videoRepository.delete(video);
        return ResponseEntity.ok(Map.of("success", true, "message", "Video deleted successfully"));
    }

    @PutMapping("/properties/{propertyId}/videos/{videoId}/featured")
    public ResponseEntity<?> setFeaturedVideo(@PathVariable Long propertyId,
                                              @PathVariable Long videoId,
                                              HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null || !property.getOwnerId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }

        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || !video.getProperty().getId().equals(propertyId)) {
            return ResponseEntity.notFound().build();
        }

        videoRepository.resetFeaturedFlags(propertyId);
        videoRepository.setFeatured(videoId);

        return ResponseEntity.ok(Map.of("success", true, "message", "Featured video updated successfully"));
    }

    @GetMapping("/properties/{propertyId}/videos")
    public ResponseEntity<?> getPropertyVideos(@PathVariable Long propertyId,
                                               HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null || !property.getOwnerId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }

        List<Video> videos = videoRepository.findByPropertyIdOrderByOrderIndexAsc(propertyId);

        // Convert to DTOs
        List<Map<String, Object>> videoDTOs = new ArrayList<>();
        for (Video video : videos) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", video.getId());
            dto.put("videoPath", video.getVideoPath());
            dto.put("title", video.getTitle());
            dto.put("description", video.getDescription());
            dto.put("isFeatured", video.getIsFeatured());
            dto.put("orderIndex", video.getOrderIndex());
            videoDTOs.add(dto);
        }

        return ResponseEntity.ok(videoDTOs);
    }
}