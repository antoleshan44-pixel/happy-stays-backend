package com.eserian.homes.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
public class PropertyRequest {
    private String title;
    private String description;
    private String location;
    private Double pricePerNight;
    private Integer bedrooms;
    private Integer bathrooms;
    private String imageUrl;
    private String propertyType;     // ADD THIS (Villa, Apartment, etc.)
    private String amenities;         // ADD THIS (JSON string like '["WiFi","Pool"]')
    private String videoPath;         // ADD THIS (optional)
    private String videoThumbnail;    // ADD THIS (optional)
}