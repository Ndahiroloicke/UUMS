package com.example.uums.dto.request;

/** Request body for registering a new meter against a customer — number, type, install date. */
import com.example.uums.enums.MeterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateMeterRequest {

    @NotBlank(message = "Meter number is required")
    private String meterNumber;

    @NotNull(message = "Meter type is required")
    private MeterType meterType;

    @NotNull(message = "Installation date is required")
    private LocalDate installationDate;

    @NotNull(message = "Customer ID is required")
    private Long customerId;
}
