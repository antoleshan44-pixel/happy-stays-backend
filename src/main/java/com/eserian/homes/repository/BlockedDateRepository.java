// src/main/java/com/eserian/homes/repository/BlockedDateRepository.java

package com.eserian.homes.repository;

import com.eserian.homes.model.BlockedDate;
import com.eserian.homes.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

public interface BlockedDateRepository extends JpaRepository<BlockedDate, Long> {

    List<BlockedDate> findByPropertyAndBlockedDateBetween(Property property, LocalDate start, LocalDate end);

    boolean existsByPropertyAndBlockedDate(Property property, LocalDate date);

    @Modifying
    @Transactional
    void deleteByPropertyAndBlockedDate(Property property, LocalDate date);

    void deleteByPropertyId(Long propertyId);
}