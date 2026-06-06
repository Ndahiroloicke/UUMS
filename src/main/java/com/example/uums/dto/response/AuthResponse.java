package com.example.uums.dto.response;

/** Response DTO returned after successful login or registration — carries JWT token and user profile summary. */
import com.example.uums.enums.UserRole;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String fullNames;
    private String email;
    private UserRole role;
}
