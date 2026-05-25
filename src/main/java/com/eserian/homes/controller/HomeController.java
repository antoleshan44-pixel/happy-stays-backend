package com.eserian.homes.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Eserian Homes API");
        response.put("status", "running");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("version", "1.0.0");
        response.put("environment", System.getenv("SPRING_PROFILES_ACTIVE") != null ?
                System.getenv("SPRING_PROFILES_ACTIVE") : "development");
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Eserian Homes Backend");
        health.put("timestamp", LocalDateTime.now().toString());
        return health;
    }
}