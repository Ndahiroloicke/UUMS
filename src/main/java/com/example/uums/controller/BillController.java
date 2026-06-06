package com.example.uums.controller;

/**
 * REST controller for utility bill lifecycle at {@code /api/bills}.
 * Supports bill generation, approval, lookup by ID/reference/customer, customer self-service
 * ({@code /me}), and manual overdue penalty triggers. Role access: ADMIN, FINANCE, CUSTOMER.
 */
import com.example.uums.dto.request.GenerateBillRequest;
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.BillResponse;
import com.example.uums.dto.response.PenaltyApplicationResponse;
import com.example.uums.service.BillService;
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
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Tag(name = "Bill Management", description = "Generate, approve and retrieve utility bills")
public class BillController {

    private final BillService billService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    @Operation(summary = "Generate a bill from a meter reading")
    public ResponseEntity<ApiResponse<BillResponse>> generateBill(
            @Valid @RequestBody GenerateBillRequest request) {
        BillResponse bill = billService.generateBill(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bill generated successfully", bill));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    @Operation(summary = "Approve a pending bill (Admin/Finance only)")
    public ResponseEntity<ApiResponse<BillResponse>> approveBill(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Bill approved successfully", billService.approveBill(id)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Operation(summary = "Get all bills for the logged-in customer (all meters)")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getMyBills() {
        return ResponseEntity.ok(ApiResponse.success(billService.getBillsForCurrentUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    @Operation(summary = "Get all bills in the system")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getAllBills() {
        return ResponseEntity.ok(ApiResponse.success(billService.getAllBills()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get bill by ID (customers can only view their own)")
    public ResponseEntity<ApiResponse<BillResponse>> getBillById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(billService.getBillById(id)));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get bill by reference number")
    public ResponseEntity<ApiResponse<BillResponse>> getBillByReference(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.success(billService.getBillByReference(reference)));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get all bills for a customer")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getBillsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(billService.getBillsByCustomer(customerId)));
    }

    @PostMapping("/apply-penalties")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    @Operation(summary = "Manually trigger overdue penalty application (Admin/Finance)")
    public ResponseEntity<ApiResponse<PenaltyApplicationResponse>> applyPenalties() {
        PenaltyApplicationResponse result = billService.applyOverduePenalties();
        String message = result.getBillsPenalized() > 0
                ? "Overdue penalties applied successfully"
                : "No bills were eligible for penalties";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
