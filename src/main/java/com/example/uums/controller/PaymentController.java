package com.example.uums.controller;

import com.example.uums.dto.request.RecordPaymentRequest;
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.PaymentResponse;
import com.example.uums.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Processing", description = "Record and view customer payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE')")
    @Operation(summary = "Record a payment (full or partial) against a bill")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request) {
        PaymentResponse payment = paymentService.recordPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", payment));
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get all payments for a bill")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByBill(@PathVariable Long billId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByBill(billId)));
    }

    @GetMapping("/customer/{customerId}/history")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get full payment history for a customer")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentHistory(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentHistoryByCustomer(customerId)));
    }
}
