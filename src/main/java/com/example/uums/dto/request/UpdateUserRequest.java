package com.example.uums.dto.request;

/** Request body for admin partial user profile update — names, phone, or status. */
import com.example.uums.enums.UserStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 255, message = "Full names must be between 2 and 255 characters")
    private String fullNames;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be 10-15 digits, optionally starting with +")
    private String phoneNumber;

    private UserStatus status;
}
