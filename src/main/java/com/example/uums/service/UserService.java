package com.example.uums.service;

import com.example.uums.dto.request.UpdateUserRequest;
import com.example.uums.dto.request.UpdateUserRoleRequest;
import com.example.uums.dto.response.UserResponse;
import com.example.uums.entity.User;
import com.example.uums.exception.BusinessRuleException;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getFullNames() != null) {
            if (request.getFullNames().isBlank()) {
                throw new BusinessRuleException("Full names cannot be blank");
            }
            user.setFullNames(request.getFullNames().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        User currentAdmin = getCurrentUser();
        if (currentAdmin != null && currentAdmin.getId().equals(id)) {
            throw new BusinessRuleException("You cannot change your own role");
        }

        if (user.getRole() == request.getRole()) {
            throw new BusinessRuleException("User already has role: " + request.getRole());
        }

        user.setRole(request.getRole());
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        User currentAdmin = getCurrentUser();
        if (currentAdmin != null && currentAdmin.getId().equals(id)) {
            throw new BusinessRuleException("You cannot delete your own account");
        }

        userRepository.deleteById(id);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private UserResponse mapToResponse(User user) {
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
