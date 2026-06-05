package com.example.uums.dto.response;

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
