package com.eserian.homes.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {
    private Long id;
    private String videoPath;
    private String title;
    private String description;
    private Boolean isFeatured;
    private Integer orderIndex;
}