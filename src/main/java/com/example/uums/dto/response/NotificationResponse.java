package com.example.uums.dto.response;

/** Response DTO for in-app customer notifications with read/unread status. */
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {
    private Long id;
    private Long customerId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
