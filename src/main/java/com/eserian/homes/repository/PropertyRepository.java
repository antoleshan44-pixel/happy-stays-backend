package com.eserian.homes.repository;

import com.eserian.homes.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    // Basic methods
    List<Property> findByStatus(String status);
    List<Property> findByOwnerId(Long ownerId);

    // ✅ JOIN FETCH methods for eager loading of photos and videos
    @Query("SELECT DISTINCT p FROM Property p " +
            "LEFT JOIN FETCH p.photos " +
            "LEFT JOIN FETCH p.videos " +
            "WHERE p.status = :status")
    List<Property> findPendingPropertiesWithMedia(@Param("status") String status);

    @Query("SELECT DISTINCT p FROM Property p " +
            "LEFT JOIN FETCH p.photos " +
            "LEFT JOIN FETCH p.videos " +
            "WHERE p.id = :id")
    Property findPropertyWithMediaById(@Param("id") Long id);

    // ✅ FIXED: Use ownerId directly (not owner.id)
    @Query("SELECT DISTINCT p FROM Property p " +
            "LEFT JOIN FETCH p.photos " +
            "LEFT JOIN FETCH p.videos " +
            "WHERE p.ownerId = :ownerId")
    List<Property> findPropertiesByOwnerWithMedia(@Param("ownerId") Long ownerId);

    @Query("SELECT DISTINCT p FROM Property p " +
            "LEFT JOIN FETCH p.photos " +
            "LEFT JOIN FETCH p.videos")
    List<Property> findAllPropertiesWithMedia();
}