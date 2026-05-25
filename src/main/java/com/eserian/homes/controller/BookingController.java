package com.eserian.homes.controller;

import com.eserian.homes.dto.BookingRequest;
import com.eserian.homes.model.Booking;
import com.eserian.homes.model.User;
import com.eserian.homes.repository.UserRepository;
import com.eserian.homes.service.BookingService;
import com.eserian.homes.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Create a booking
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            User user = getCurrentUser(httpRequest);
            Booking booking = bookingService.createBooking(request, user);
            return ResponseEntity.ok("Booking confirmed! Total: $" + booking.getTotalPrice());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get my bookings
    @GetMapping("/my-bookings")
    public ResponseEntity<List<Booking>> getMyBookings(HttpServletRequest request) {
        User user = getCurrentUser(request);
        return ResponseEntity.ok(bookingService.getUserBookings(user));
    }

    // Cancel booking
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId,
                                           HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            bookingService.cancelBooking(bookingId, user);
            return ResponseEntity.ok("Booking cancelled");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}