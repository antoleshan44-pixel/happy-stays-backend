// src/main/java/com/eserian/homes/repository/BookingRepository.java

package com.eserian.homes.repository;

import com.eserian.homes.model.Booking;
import com.eserian.homes.model.Property;
import com.eserian.homes.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ============================================
    // FIND BY RELATIONSHIPS
    // ============================================

    /**
     * Find all bookings for a specific user (customer)
     */
    List<Booking> findByUser(User user);

    /**
     * Find all bookings for a specific user ID
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Find all bookings for a specific property
     */
    List<Booking> findByProperty(Property property);

    /**
     * Find all bookings for a specific property ID
     */
    List<Booking> findByPropertyId(Long propertyId);

    /**
     * Find booking by ID with eager loading of user and property
     */
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.user LEFT JOIN FETCH b.property WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    // ============================================
    // FIND BY STATUS
    // ============================================

    /**
     * Find bookings by status
     */
    List<Booking> findByStatus(String status);

    /**
     * Find confirmed bookings for a property
     */
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * Find pending bookings for a property
     */
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId AND b.status = 'PENDING'")
    List<Booking> findPendingByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * Find completed bookings for a user
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = 'COMPLETED'")
    List<Booking> findCompletedByUserId(@Param("userId") Long userId);

    // ============================================
    // DATE RANGE QUERIES (AVAILABILITY CHECKING)
    // ============================================

    /**
     * Find conflicting bookings for a property within a date range
     * Used to prevent double booking
     */
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status != 'CANCELLED' " +
            "AND ((b.checkInDate BETWEEN :checkIn AND :checkOut) " +
            "OR (b.checkOutDate BETWEEN :checkIn AND :checkOut) " +
            "OR (:checkIn BETWEEN b.checkInDate AND b.checkOutDate))")
    List<Booking> findConflictingBookings(@Param("propertyId") Long propertyId,
                                          @Param("checkIn") LocalDate checkIn,
                                          @Param("checkOut") LocalDate checkOut);

    /**
     * Find bookings that overlap with a specific date range for a property
     */
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status IN ('CONFIRMED', 'PENDING') " +
            "AND b.checkInDate <= :endDate AND b.checkOutDate >= :startDate")
    List<Booking> findByPropertyIdAndDateRange(@Param("propertyId") Long propertyId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * Find bookings for a property on a specific date
     */
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status = 'CONFIRMED' " +
            "AND :date BETWEEN b.checkInDate AND b.checkOutDate")
    List<Booking> findByPropertyIdAndDate(@Param("propertyId") Long propertyId,
                                          @Param("date") LocalDate date);

    // ============================================
    // UPCOMING & PAST BOOKINGS
    // ============================================

    /**
     * Find upcoming bookings for a user (check-in date >= today)
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId " +
            "AND b.checkInDate >= CURRENT_DATE " +
            "AND b.status IN ('CONFIRMED', 'PENDING') " +
            "ORDER BY b.checkInDate ASC")
    List<Booking> findUpcomingByUserId(@Param("userId") Long userId);

    /**
     * Find past bookings for a user (check-out date < today)
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId " +
            "AND b.checkOutDate < CURRENT_DATE " +
            "ORDER BY b.checkOutDate DESC")
    List<Booking> findPastByUserId(@Param("userId") Long userId);

    /**
     * Find upcoming bookings for an owner's properties
     */
    @Query("SELECT b FROM Booking b WHERE b.property.ownerId = :ownerId " +
            "AND b.checkInDate >= CURRENT_DATE " +
            "AND b.status IN ('CONFIRMED', 'PENDING') " +
            "ORDER BY b.checkInDate ASC")
    List<Booking> findUpcomingByOwnerId(@Param("ownerId") Long ownerId);

    // ============================================
    // TIME-BASED QUERIES
    // ============================================

    /**
     * Find bookings created after a specific date (for analytics)
     */
    List<Booking> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find bookings for a property created after a specific date
     */
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId AND b.createdAt > :date")
    List<Booking> findByPropertyIdAndCreatedAtAfter(@Param("propertyId") Long propertyId,
                                                    @Param("date") LocalDateTime date);

    /**
     * Find bookings between two dates
     */
    @Query("SELECT b FROM Booking b WHERE b.createdAt BETWEEN :startDate AND :endDate")
    List<Booking> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // ============================================
    // COUNT QUERIES
    // ============================================

    /**
     * Count total bookings for a property
     */
    long countByPropertyId(Long propertyId);

    /**
     * Count confirmed bookings for a property
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.property.id = :propertyId AND b.status = 'CONFIRMED'")
    long countConfirmedByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * Count completed bookings for a property
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.property.id = :propertyId AND b.status = 'COMPLETED'")
    long countCompletedByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * Count total bookings for an owner
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.property.ownerId = :ownerId")
    long countByOwnerId(@Param("ownerId") Long ownerId);

    // ============================================
    // EARNINGS & REVENUE QUERIES
    // ============================================

    /**
     * Get total revenue for a property from completed bookings
     */
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
            "WHERE b.property.id = :propertyId AND b.status = 'COMPLETED'")
    Double getTotalRevenueByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * Get total revenue for an owner from completed bookings
     */
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
            "WHERE b.property.ownerId = :ownerId AND b.status = 'COMPLETED'")
    Double getTotalRevenueByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Get revenue for a property in a specific year
     */
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
            "WHERE b.property.id = :propertyId " +
            "AND b.status = 'COMPLETED' " +
            "AND YEAR(b.createdAt) = :year")
    Double getRevenueByPropertyIdAndYear(@Param("propertyId") Long propertyId,
                                         @Param("year") int year);

    /**
     * Get monthly revenue for a property
     */
    @Query("SELECT MONTH(b.createdAt) as month, COALESCE(SUM(b.totalPrice), 0) as revenue " +
            "FROM Booking b " +
            "WHERE b.property.id = :propertyId " +
            "AND b.status = 'COMPLETED' " +
            "AND YEAR(b.createdAt) = :year " +
            "GROUP BY MONTH(b.createdAt)")
    List<Object[]> getMonthlyRevenueByPropertyId(@Param("propertyId") Long propertyId,
                                                 @Param("year") int year);

    // ============================================
    // UPDATE QUERIES
    // ============================================

    /**
     * Update booking status
     */
    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.status = :status, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :id")
    int updateBookingStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * Cancel all pending bookings for a property (used when property is suspended)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.status = 'CANCELLED', b.cancellationReason = :reason " +
            "WHERE b.property.id = :propertyId AND b.status = 'PENDING'")
    int cancelPendingBookingsByPropertyId(@Param("propertyId") Long propertyId,
                                          @Param("reason") String reason);

    /**
     * Mark booking as completed after checkout date
     */
    @Modifying
    @Transactional
    @Query("UPDATE Booking b SET b.status = 'COMPLETED' " +
            "WHERE b.status = 'CONFIRMED' AND b.checkOutDate < CURRENT_DATE")
    int markCompletedBookings();

    // ============================================
    // CHECKING METHODS
    // ============================================

    /**
     * Check if a property has any confirmed bookings
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.property.id = :propertyId AND b.status IN ('CONFIRMED', 'PENDING')")
    boolean hasActiveBookings(@Param("propertyId") Long propertyId);

    /**
     * Check if a user has booked a specific property
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.user.id = :userId AND b.property.id = :propertyId AND b.status = 'CONFIRMED'")
    boolean hasUserBookedProperty(@Param("userId") Long userId,
                                  @Param("propertyId") Long propertyId);
}