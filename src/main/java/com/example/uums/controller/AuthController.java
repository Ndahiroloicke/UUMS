package com.example.uums.controller;

/**
 * REST controller for public authentication endpoints at {@code /api/auth}.
 * Handles user registration (creates ROLE_CUSTOMER accounts) and login (returns JWT).
 * These endpoints are unauthenticated; all other API routes require a valid token.
 */
import com.example.uums.dto.request.LoginRequest;
import com.example.uums.dto.request.RegisterUserRequest;
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.AuthResponse;
import com.example.uums.dto.response.UserResponse;
import com.example.uums.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and login endpoints")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account (automatically assigned ROLE_CUSTOMER)")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully. You have been assigned the CUSTOMER role.", user));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
