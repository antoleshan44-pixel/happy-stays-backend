// File: src/main/java/com/eserian/homes/controller/AdminAuthController.java
// LOCATION: BACKEND - Spring Boot REST Controller

package com.eserian.homes.controller;

import com.eserian.homes.dto.LoginRequest;
import com.eserian.homes.entity.Admin;
import com.eserian.homes.service.AdminService;
import com.eserian.homes.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;  // ← CHANGED: javax.servlet → jakarta.servlet
import jakarta.validation.Valid;  // ← CHANGED: javax.validation → jakarta.validation
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminAuthController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Optional<Admin> adminOpt = adminService.authenticate(request.getEmail(), request.getPassword());

        if (!adminOpt.isPresent()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid credentials or account is not active");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        Admin admin = adminOpt.get();

        // Update last login
        String clientIp = getClientIp(httpRequest);
        adminService.updateLastLogin(admin.getId(), clientIp);

        // Generate JWT token
        String token = jwtUtil.generateToken(admin.getEmail(), admin.getRole(), admin.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("admin", adminService.getAdminWithPermissions(admin));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("roles", adminService.getAllRoles());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);

            Optional<Admin> adminOpt = adminService.getAdminByEmail(email);
            if (adminOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("admin", adminService.getAdminWithPermissions(adminOpt.get()));
                return ResponseEntity.ok(response);
            }
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}