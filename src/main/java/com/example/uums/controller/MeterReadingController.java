package com.example.uums.controller;

/**
 * REST controller for meter reading capture and queries at {@code /api/meter-readings}.
 * Admin/Operator staff capture monthly consumption readings; staff and customers
 * retrieve readings by meter or ID. Readings drive bill generation.
 */
import com.example.uums.dto.request.CaptureMeterReadingRequest;
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.MeterReadingResponse;
import com.example.uums.service.MeterReadingService;
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
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
@Tag(name = "Meter Readings", description = "Capture and retrieve meter readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "Capture a meter reading (Admin/Operator only)")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> captureReading(
            @Valid @RequestBody CaptureMeterReadingRequest request) {
        MeterReadingResponse reading = meterReadingService.captureReading(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter reading captured successfully", reading));
    }

    @GetMapping("/meter/{meterId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get readings for a specific meter")
    public ResponseEntity<ApiResponse<List<MeterReadingResponse>>> getReadingsByMeter(@PathVariable Long meterId) {
        return ResponseEntity.ok(ApiResponse.success(meterReadingService.getReadingsByMeter(meterId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_FINANCE')")
    @Operation(summary = "Get meter reading by ID")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> getReadingById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(meterReadingService.getReadingById(id)));
    }
}
