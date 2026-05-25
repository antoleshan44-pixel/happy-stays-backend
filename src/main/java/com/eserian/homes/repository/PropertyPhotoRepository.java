// src/main/java/com/eserian/homes/repository/PropertyPhotoRepository.java

package com.eserian.homes.repository;

import com.eserian.homes.model.PropertyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface PropertyPhotoRepository extends JpaRepository<PropertyPhoto, Long> {

    List<PropertyPhoto> findByPropertyIdOrderByOrderIndexAsc(Long propertyId);

    int countByPropertyId(Long propertyId);

    @Modifying
    @Transactional
    @Query("UPDATE PropertyPhoto p SET p.isPrimary = false WHERE p.property.id = :propertyId")
    void resetPrimaryFlags(@Param("propertyId") Long propertyId);

    @Modifying
    @Transactional
    @Query("UPDATE PropertyPhoto p SET p.isPrimary = true WHERE p.id = :photoId")
    void setPrimary(@Param("photoId") Long photoId);

    void deleteByPropertyId(Long propertyId);
}