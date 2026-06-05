package com.example.uums.dto.request;

import com.example.uums.enums.UserStatus;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateUserRequest {
    private String fullNames;
    private String phoneNumber;
    private UserStatus status;
}
