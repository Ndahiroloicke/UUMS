package com.example.uums.dto.request;

import com.example.uums.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private UserRole role;
}
