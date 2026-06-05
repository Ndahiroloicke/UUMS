package com.example.uums.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GenerateBillRequest {

    @NotNull(message = "Meter ID is required")
    private Long meterId;

    @NotNull(message = "Billing year is required")
    @Min(value = 2000, message = "Year must be valid")
    private Integer billingYear;

    @NotNull(message = "Billing month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer billingMonth;
}
