// src/main/java/com/eserian/homes/repository/VideoRepository.java

package com.eserian.homes.repository;

import com.eserian.homes.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByPropertyIdOrderByOrderIndexAsc(Long propertyId);

    int countByPropertyId(Long propertyId);

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.isFeatured = false WHERE v.property.id = :propertyId")
    void resetFeaturedFlags(@Param("propertyId") Long propertyId);

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.isFeatured = true WHERE v.id = :videoId")
    void setFeatured(@Param("videoId") Long videoId);

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.orderIndex = :orderIndex WHERE v.id = :videoId")
    void updateOrderIndex(@Param("videoId") Long videoId, @Param("orderIndex") int orderIndex);

    void deleteByPropertyId(Long propertyId);
}