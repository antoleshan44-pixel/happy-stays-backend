// File: src/main/java/com/eserian/homes/model/BlockedDate.java
// LOCATION: C:\Users\antol\programming projects\EserianHomes-Clean\backend\src\main\java\com\eserian\homes\model\BlockedDate.java

package com.eserian.homes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_dates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @JsonIgnore
    private Property property;

    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}