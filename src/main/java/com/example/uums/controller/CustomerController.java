package com.example.uums.controller;

/**
 * REST controller for customer management at {@code /api/customers}.
 * Admins create, update, and deactivate customers; staff can list and view records;
 * customers can view their own profile via a dedicated endpoint.
 */
import com.example.uums.dto.request.CreateCustomerRequest;
import com.example.uums.dto.request.UpdateCustomerRequest;
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.CustomerResponse;
import com.example.uums.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Manage customer records")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new customer (Admin only)")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", customer));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_FINANCE')")
    @Operation(summary = "Get all customers")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.success(customerService.getAllCustomers()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomerById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update customer (Admin only)")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully",
                customerService.updateCustomer(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Deactivate customer (Admin only)")
    public ResponseEntity<ApiResponse<CustomerResponse>> deactivateCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Customer deactivated",
                customerService.deactivateCustomer(id)));
    }
}
