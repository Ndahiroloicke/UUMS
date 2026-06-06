package com.example.uums.dto.request;

/** Request body for creating a versioned pricing tariff with optional tier-based consumption brackets. */
import com.example.uums.enums.MeterType;
import com.example.uums.enums.TariffType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateTariffRequest {

    @NotBlank(message = "Tariff name is required")
    private String name;

    @NotNull(message = "Meter type is required")
    private MeterType meterType;

    @NotNull(message = "Tariff type is required")
    private TariffType tariffType;

    private BigDecimal flatRate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private List<TariffTierRequest> tiers;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TariffTierRequest {
        @NotNull(message = "Min consumption is required")
        private BigDecimal minConsumption;
        private BigDecimal maxConsumption;
        @NotNull(message = "Rate is required")
        private BigDecimal rate;
        @NotNull(message = "Tier order is required")
        private Integer tierOrder;
    }
}
