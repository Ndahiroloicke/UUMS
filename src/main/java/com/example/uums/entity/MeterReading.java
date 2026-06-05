package com.example.uums.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_readings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @Column(name = "previous_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal previousReading;

    @Column(name = "current_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentReading;

    @Column(name = "consumption", nullable = false, precision = 12, scale = 2)
    private BigDecimal consumption;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captured_by_id")
    private User capturedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentReading != null && previousReading != null) {
            consumption = currentReading.subtract(previousReading);
        }
    }
}
