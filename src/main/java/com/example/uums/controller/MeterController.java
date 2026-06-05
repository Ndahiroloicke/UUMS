package com.example.uums.controller;

import com.example.uums.dto.request.CreateMeterRequest;
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.MeterResponse;
import com.example.uums.enums.MeterStatus;
import com.example.uums.service.MeterService;
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
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Tag(name = "Meter Management", description = "Manage utility meters")
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Register a new meter (Admin only)")
    public ResponseEntity<ApiResponse<MeterResponse>> createMeter(
            @Valid @RequestBody CreateMeterRequest request) {
        MeterResponse meter = meterService.createMeter(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter registered successfully", meter));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_FINANCE')")
    @Operation(summary = "Get all meters")
    public ResponseEntity<ApiResponse<List<MeterResponse>>> getAllMeters() {
        return ResponseEntity.ok(ApiResponse.success(meterService.getAllMeters()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get meter by ID")
    public ResponseEntity<ApiResponse<MeterResponse>> getMeterById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(meterService.getMeterById(id)));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get all meters for a customer")
    public ResponseEntity<ApiResponse<List<MeterResponse>>> getMetersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(meterService.getMetersByCustomer(customerId)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update meter status (Admin only)")
    public ResponseEntity<ApiResponse<MeterResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam MeterStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Meter status updated",
                meterService.updateMeterStatus(id, status)));
    }
}
