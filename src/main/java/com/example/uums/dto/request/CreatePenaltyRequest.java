package com.example.uums.dto.request;

/** Request body for defining a late-payment penalty rule — rate, grace period, and effective date. */
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreatePenaltyRequest {

    @NotBlank(message = "Penalty name is required")
    private String name;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0001", message = "Rate must be greater than 0")
    @DecimalMax(value = "0.9999", message = "Rate must be less than 1")
    private BigDecimal rate;

    private Integer gracePeriodDays = 30;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
}
