package com.eserian.homes.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
public class BookingRequest {
    private Long propertyId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}