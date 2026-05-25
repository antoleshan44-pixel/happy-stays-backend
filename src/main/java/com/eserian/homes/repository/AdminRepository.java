// File: src/main/java/com/eserian/homes/repository/AdminRepository.java
// LOCATION: BACKEND - Spring Boot Repository

package com.eserian.homes.repository;

import com.eserian.homes.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Admin> findByEmailAndStatus(String email, String status);
}