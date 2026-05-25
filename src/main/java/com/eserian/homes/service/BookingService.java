// src/main/java/com/eserian/homes/service/BookingService.java

package com.eserian.homes.service;

import com.eserian.homes.dto.BookingRequest;
import com.eserian.homes.model.Booking;
import com.eserian.homes.model.Property;
import com.eserian.homes.model.User;
import com.eserian.homes.repository.BookingRepository;
import com.eserian.homes.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    // User books a property
    public Booking createBooking(BookingRequest request, User user) {
        // Get the property
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Check if property is approved
        if (!property.getStatus().equals("APPROVED")) {
            throw new RuntimeException("Property is not available for booking");
        }

        // Check for date conflicts using property ID
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                property.getId(),  // Pass property.getId() instead of property object
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Property already booked for these dates");
        }

        // Calculate total price
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        Double totalPrice = property.getPricePerNight() * nights;

        // Create booking
        Booking booking = new Booking();
        booking.setProperty(property);
        booking.setUser(user);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setTotalPrice(totalPrice);
        booking.setStatus("CONFIRMED");
        booking.setCreatedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    // Get user's bookings
    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUser(user);
    }

    // Cancel booking
    public void cancelBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Only the user who booked or admin can cancel
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to cancel this booking");
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }
}