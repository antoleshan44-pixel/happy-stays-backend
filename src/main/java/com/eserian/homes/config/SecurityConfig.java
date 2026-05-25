// File: src/main/java/com/eserian/homes/config/SecurityConfig.java
// LOCATION: BACKEND - Spring Boot Security Configuration
// UPDATED - Added H2 console support for Render deployment

package com.eserian.homes.config;

import com.eserian.homes.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8000",
                "http://localhost:8080",
                "https://eserian-homes1.vercel.app"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ============================================
                        // PUBLIC ENDPOINTS - Anyone can access
                        // ============================================
                        .requestMatchers("/", "/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/health").permitAll()

                        // H2 Console (for development on Render)
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/properties").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/properties/approved").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/properties/{id}").permitAll()

                        // ============================================
                        // STATIC RESOURCES (Images, Videos) - Allow public access
                        // ============================================
                        .requestMatchers("/properties/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // ============================================
                        // CUSTOMER ENDPOINTS - Only CUSTOMER role
                        // ============================================
                        .requestMatchers(HttpMethod.POST, "/api/bookings").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/my-bookings").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/bookings/{bookingId}").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()

                        // ============================================
                        // OWNER ENDPOINTS - All start with /api/owner/
                        // ============================================
                        .requestMatchers("/api/owner/**").hasRole("OWNER")

                        // ============================================
                        // ADMIN ENDPOINTS - All start with /api/admin/
                        // ============================================
                        .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ============================================
                        // ANY OTHER REQUEST - Require authentication
                        // ============================================
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}