package com.example.uums.dto.response;

/** Response DTO exposing full bill details: charges breakdown, payment totals, status, and approval metadata. */
import com.example.uums.enums.BillStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillResponse {
    private Long id;
    private String billReference;
    private Long customerId;
    private String customerName;
    private Long meterId;
    private String meterNumber;
    private String meterType;
    private LocalDate billingPeriod;
    private BigDecimal consumption;
    private BigDecimal consumptionAmount;
    private BigDecimal serviceChargeAmount;
    private BigDecimal taxAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal outstandingBalance;
    private BillStatus status;
    private LocalDate dueDate;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
