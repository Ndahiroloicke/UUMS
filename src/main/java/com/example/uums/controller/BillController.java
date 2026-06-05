package com.example.uums.controller;

import com.example.uums.dto.request.GenerateBillRequest;
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.BillResponse;
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

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    @Operation(summary = "Get all bills")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getAllBills() {
        return ResponseEntity.ok(ApiResponse.success(billService.getAllBills()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get bill by ID")
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
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Manually trigger overdue penalty application via DB stored procedure")
    public ResponseEntity<ApiResponse<Void>> applyPenalties() {
        billService.applyOverduePenalties();
        return ResponseEntity.ok(ApiResponse.success("Overdue penalties applied successfully", null));
    }
}
