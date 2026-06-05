package com.example.uums.dto.response;

import com.example.uums.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponse {
    private Long id;
    private String paymentReference;
    private Long billId;
    private String billReference;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private LocalDateTime paymentDate;
    private String processedByName;
    private BigDecimal outstandingBalance;
    private LocalDateTime createdAt;
}
