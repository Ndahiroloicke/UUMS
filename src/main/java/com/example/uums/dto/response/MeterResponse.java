package com.example.uums.dto.response;

import com.example.uums.enums.MeterStatus;
import com.example.uums.enums.MeterType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeterResponse {
    private Long id;
    private String meterNumber;
    private MeterType meterType;
    private LocalDate installationDate;
    private MeterStatus status;
    private Long customerId;
    private String customerName;
    private LocalDateTime createdAt;
}
