package com.example.uums.controller;

import com.example.uums.dto.request.*;
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.TariffResponse;
import com.example.uums.entity.Penalty;
import com.example.uums.entity.ServiceCharge;
import com.example.uums.entity.Tax;
import com.example.uums.service.TariffService;
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
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@Tag(name = "Tariff Configuration", description = "Manage tariffs, service charges, taxes, and penalties (Admin only)")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @Operation(summary = "Create a new tariff (versioned)")
    public ResponseEntity<ApiResponse<TariffResponse>> createTariff(
            @Valid @RequestBody CreateTariffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tariff created successfully", tariffService.createTariff(request)));
    }

    @GetMapping
    @Operation(summary = "Get all tariffs")
    public ResponseEntity<ApiResponse<List<TariffResponse>>> getAllTariffs() {
        return ResponseEntity.ok(ApiResponse.success(tariffService.getAllTariffs()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tariff by ID")
    public ResponseEntity<ApiResponse<TariffResponse>> getTariffById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tariffService.getTariffById(id)));
    }

    // ---- Service Charges ----

    @PostMapping("/service-charges")
    @Operation(summary = "Create a service charge")
    public ResponseEntity<ApiResponse<ServiceCharge>> createServiceCharge(
            @Valid @RequestBody CreateServiceChargeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service charge created", tariffService.createServiceCharge(request)));
    }

    @GetMapping("/service-charges")
    @Operation(summary = "Get all service charges")
    public ResponseEntity<ApiResponse<List<ServiceCharge>>> getAllServiceCharges() {
        return ResponseEntity.ok(ApiResponse.success(tariffService.getAllServiceCharges()));
    }

    // ---- Taxes ----

    @PostMapping("/taxes")
    @Operation(summary = "Create a tax (e.g. VAT)")
    public ResponseEntity<ApiResponse<Tax>> createTax(@Valid @RequestBody CreateTaxRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tax created", tariffService.createTax(request)));
    }

    @GetMapping("/taxes")
    @Operation(summary = "Get all taxes")
    public ResponseEntity<ApiResponse<List<Tax>>> getAllTaxes() {
        return ResponseEntity.ok(ApiResponse.success(tariffService.getAllTaxes()));
    }

    // ---- Penalties ----

    @PostMapping("/penalties")
    @Operation(summary = "Create a late payment penalty")
    public ResponseEntity<ApiResponse<Penalty>> createPenalty(@Valid @RequestBody CreatePenaltyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Penalty created", tariffService.createPenalty(request)));
    }

    @GetMapping("/penalties")
    @Operation(summary = "Get all penalties")
    public ResponseEntity<ApiResponse<List<Penalty>>> getAllPenalties() {
        return ResponseEntity.ok(ApiResponse.success(tariffService.getAllPenalties()));
    }
}
