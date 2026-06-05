package com.example.uums.service;

import com.example.uums.dto.request.LoginRequest;
import com.example.uums.dto.request.RegisterUserRequest;
import com.example.uums.dto.response.AuthResponse;
import com.example.uums.dto.response.UserResponse;
import com.example.uums.entity.User;
import com.example.uums.enums.UserRole;
import com.example.uums.enums.UserStatus;
import com.example.uums.exception.BusinessRuleException;
import com.example.uums.exception.DuplicateResourceException;
import com.example.uums.repository.UserRepository;
import com.example.uums.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }

        User user = User.builder()
                .fullNames(request.getFullNames().trim())
                .email(email)
                .phoneNumber(request.getPhoneNumber().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        return mapToUserResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessRuleException("Account is inactive. Please contact admin.");
        }

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullNames(user.getFullNames())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullNames(user.getFullNames())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
