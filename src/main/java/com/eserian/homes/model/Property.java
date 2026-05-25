// src/main/java/com/eserian/homes/model/Property.java

package com.eserian.homes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "price_per_night", nullable = false)
    private Double pricePerNight;

    private Integer bedrooms = 1;
    private Integer bathrooms = 1;

    @Column(name = "image_url")
    private String imageUrl;

    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(name = "property_type", length = 50)
    private String propertyType;

    @Column(name = "video_path")
    private String videoPath;

    @Column(name = "video_thumbnail")
    private String videoThumbnail;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "admin_status")
    private String adminStatus = "active";

    @Column(name = "featured")
    private Boolean featured = false;

    @Column(name = "archived_data", columnDefinition = "TEXT")
    private String archivedData;

    // ========== APPROVAL FIELDS ==========
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "moderator_notes", columnDefinition = "TEXT")
    private String moderatorNotes;

    // ========== REJECTION FIELDS ==========
    @Column(name = "rejection_reason", length = 100)
    private String rejectionReason;

    @Column(name = "rejection_details", columnDefinition = "TEXT")
    private String rejectionDetails;

    @Column(name = "allow_resubmission")
    private Boolean allowResubmission = false;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    // ========== RELATIONSHIPS ==========
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PropertyPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Video> videos = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BlockedDate> blockedDates = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Booking> bookings = new ArrayList<>();

    // ========== TRANSIENT FIELDS ==========
    @Transient
    private Integer photoCount = 0;

    @Transient
    private Integer videoCount = 0;

    @Transient
    private Double averageRating = 0.0;

    @Transient
    private Integer totalReviews = 0;

    @Transient
    private String mainPhotoUrl;

    @Transient
    private String featuredVideoUrl;

    // ========== HELPER METHODS ==========
    public void setPhotoCount(int count) { this.photoCount = count; }
    public void setVideoCount(int count) { this.videoCount = count; }
    public void setAverageRating(Double rating) { this.averageRating = rating; }
    public void setTotalReviews(int count) { this.totalReviews = count; }
    public void setMainPhotoUrl(String url) { this.mainPhotoUrl = url; }
    public void setFeaturedVideoUrl(String url) { this.featuredVideoUrl = url; }

    // Risk Score getter/setter
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (adminStatus == null) adminStatus = "active";
        if (viewCount == null) viewCount = 0;
        if (featured == null) featured = false;
        if (bedrooms == null) bedrooms = 1;
        if (bathrooms == null) bathrooms = 1;
        if (riskScore == null) riskScore = 0;
        if (allowResubmission == null) allowResubmission = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    public boolean isApproved() { return "APPROVED".equals(status); }
    public boolean isActive() { return "APPROVED".equals(status) && "active".equals(adminStatus); }
    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isRejected() { return "REJECTED".equals(status); }
    public boolean isSuspended() { return "suspended".equals(adminStatus); }
    public boolean isArchived() { return "archived".equals(adminStatus); }

    public List<String> getAmenitiesList() {
        if (amenities == null || amenities.isEmpty()) return new ArrayList<>();
        try {
            String clean = amenities.replace("[", "").replace("]", "").replace("\"", "");
            if (clean.isEmpty()) return new ArrayList<>();
            return List.of(clean.split(","));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setAmenitiesList(List<String> amenitiesList) {
        if (amenitiesList == null || amenitiesList.isEmpty()) {
            this.amenities = "[]";
        } else {
            this.amenities = amenitiesList.toString();
        }
    }

    // Convenience methods
    public Integer getPhotoCount() {
        return photos != null ? photos.size() : 0;
    }

    public Integer getVideoCount() {
        return videos != null ? videos.size() : 0;
    }

    public String getMainPhotoUrl() {
        if (photos != null && !photos.isEmpty()) {
            for (PropertyPhoto photo : photos) {
                if (photo.getIsPrimary() != null && photo.getIsPrimary()) {
                    return photo.getPhotoPath();
                }
            }
            return photos.get(0).getPhotoPath();
        }
        return imageUrl;
    }

    public String getFeaturedVideoUrl() {
        if (videos != null && !videos.isEmpty()) {
            for (Video video : videos) {
                if (video.getIsFeatured() != null && video.getIsFeatured()) {
                    return video.getVideoPath();
                }
            }
            return videos.get(0).getVideoPath();
        }
        return videoPath;
    }
}