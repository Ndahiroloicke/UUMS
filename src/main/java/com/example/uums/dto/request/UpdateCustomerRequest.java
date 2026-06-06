package com.example.uums.dto.request;

/** Request body for partial customer profile update — all fields optional. */
import com.example.uums.enums.CustomerStatus;
import jakarta.validation.constraints.Email;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateCustomerRequest {
    private String fullNames;
    @Email(message = "Invalid email format")
    private String email;
    private String phoneNumber;
    private String address;
    private CustomerStatus status;
}
