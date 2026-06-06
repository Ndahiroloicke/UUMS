package com.example.uums.dto.response;

/** Response DTO for customer profile data returned by customer API endpoints. */
import com.example.uums.enums.CustomerStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerResponse {
    private Long id;
    private String fullNames;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private String address;
    private CustomerStatus status;
    private Long userId;
    private LocalDateTime createdAt;
}
