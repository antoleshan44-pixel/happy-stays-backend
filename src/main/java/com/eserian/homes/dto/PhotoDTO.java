package com.eserian.homes.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDTO {
    private Long id;
    private String photoPath;
    private Boolean isPrimary;
    private Integer orderIndex;
}