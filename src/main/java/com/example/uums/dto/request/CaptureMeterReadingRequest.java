package com.example.uums.dto.request;

/** Request body for capturing a new meter reading — meter ID, current reading value, and date. */
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CaptureMeterReadingRequest {

    @NotNull(message = "Meter ID is required")
    private Long meterId;

    @NotNull(message = "Current reading is required")
    @Positive(message = "Current reading must be positive")
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    private LocalDate readingDate;
}
