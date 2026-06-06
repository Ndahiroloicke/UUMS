package com.example.uums.dto.request;

/** Request body for creating a fixed service charge per meter type (water or electricity). */
import com.example.uums.enums.MeterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateServiceChargeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Meter type is required")
    private MeterType meterType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
}
