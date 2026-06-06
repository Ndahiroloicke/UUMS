package com.example.uums.entity;

/**
 * JPA entity for a single consumption bracket within a tier-based tariff.
 * Defines min/max consumption bounds, rate per unit, and display order within the tariff.
 */
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tariff_tiers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TariffTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @Column(name = "min_consumption", nullable = false, precision = 12, scale = 2)
    private BigDecimal minConsumption;

    @Column(name = "max_consumption", precision = 12, scale = 2)
    private BigDecimal maxConsumption;

    @Column(name = "rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal rate;

    @Column(name = "tier_order", nullable = false)
    private Integer tierOrder;
}
