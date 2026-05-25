package com.eserian.homes.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDTO {
    private Long id;
    private String title;
    private String description;
    private String location;
    private Double pricePerNight;
    private Integer bedrooms;
    private Integer bathrooms;
    private String imageUrl;
    private String status;
    private String amenities;
    private String propertyType;
    private String videoPath;
    private String videoThumbnail;
    private Long ownerId;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String adminStatus;
    private Boolean featured;
    private Integer photoCount;
    private Integer videoCount;
    private Double averageRating;
    private Integer totalReviews;
    private String mainPhotoUrl;
    private String featuredVideoUrl;
    private List<PhotoDTO> photos;
    private List<VideoDTO> videos;
}