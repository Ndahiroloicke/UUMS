package com.example.uums.dto.request;

/** Request body for creating a new utility customer with identity, contact info, and optional user link. */
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateCustomerRequest {

    @NotBlank(message = "Full names are required")
    private String fullNames;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private String address;

    private Long userId;
}
