package com.example.uums.dto.response;

/** Response DTO for tariff configuration including nested tier details for tier-based pricing. */
import com.example.uums.enums.MeterType;
import com.example.uums.enums.TariffType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TariffResponse {
    private Long id;
    private String name;
    private MeterType meterType;
    private TariffType tariffType;
    private BigDecimal flatRate;
    private Integer version;
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private Boolean isActive;
    private List<TariffTierResponse> tiers;
    private LocalDateTime createdAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TariffTierResponse {
        private Long id;
        private BigDecimal minConsumption;
        private BigDecimal maxConsumption;
        private BigDecimal rate;
        private Integer tierOrder;
    }
}
