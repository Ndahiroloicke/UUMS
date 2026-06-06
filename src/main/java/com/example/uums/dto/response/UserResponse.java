package com.example.uums.dto.response;

/** Response DTO for system user profile data (excludes password and sensitive fields). */
import com.example.uums.enums.UserRole;
import com.example.uums.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String fullNames;
    private String email;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
}
