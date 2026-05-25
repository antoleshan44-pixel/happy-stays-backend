package com.eserian.homes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.eserian.homes.model,com.eserian.homes.entity")
@EnableJpaRepositories(basePackages = "com.eserian.homes.repository")
public class EserianHomesApplication {
    public static void main(String[] args) {
        SpringApplication.run(EserianHomesApplication.class, args);
        System.out.println("========================================");
        System.out.println("  Eserian Homes Backend Started!");
        System.out.println("  Admin Login: admin@eserian.com / Admin123");
        System.out.println("========================================");
    }
}