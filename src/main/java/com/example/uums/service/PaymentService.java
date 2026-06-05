package com.example.uums.service;

import com.example.uums.dto.request.RecordPaymentRequest;
import com.example.uums.dto.response.PaymentResponse;
import com.example.uums.entity.Bill;
import com.example.uums.entity.Payment;
import com.example.uums.entity.User;
import com.example.uums.enums.BillStatus;
import com.example.uums.exception.BusinessRuleException;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.BillRepository;
import com.example.uums.repository.PaymentRepository;
import com.example.uums.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request) {
        Bill bill = billRepository.findByBillReference(request.getBillReference())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + request.getBillReference()));

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessRuleException("Bill " + request.getBillReference() + " is already fully paid.");
        }

        // Cap payment at outstanding balance (support partial payments)
        BigDecimal amountToPay = request.getAmountPaid().min(bill.getOutstandingBalance());

        User processedBy = getCurrentUser();

        Payment payment = Payment.builder()
                .paymentReference(generatePaymentReference())
                .bill(bill)
                .amountPaid(amountToPay)
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(LocalDateTime.now())
                .processedBy(processedBy)
                .build();

        // Update bill
        BigDecimal newAmountPaid = bill.getAmountPaid().add(amountToPay);
        BigDecimal newBalance = bill.getOutstandingBalance().subtract(amountToPay);

        bill.setAmountPaid(newAmountPaid);
        bill.setOutstandingBalance(newBalance.max(BigDecimal.ZERO));

        // The DB trigger (BEFORE UPDATE on bills) automatically sets status=PAID when balance<=0
        // We also set it here to keep the JPA state consistent
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            bill.setStatus(BillStatus.PAID);
        }

        billRepository.save(bill);
        Payment savedPayment = paymentRepository.save(payment);

        // Send email if fully paid
        if (bill.getStatus() == BillStatus.PAID) {
            String period = bill.getBillingPeriod().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            String message = "Dear " + bill.getCustomer().getFullNames() +
                    ",\nYour " + period + " utility bill of " + bill.getTotalAmount() +
                    " FRW has been successfully processed.";
            notificationService.sendEmailSilently(
                    bill.getCustomer().getEmail(),
                    "Payment Confirmation - " + bill.getBillReference(),
                    message);
        }

        // Re-fetch to get trigger-updated status
        Bill refreshed = billRepository.findById(bill.getId()).orElse(bill);

        return PaymentResponse.builder()
                .id(savedPayment.getId())
                .paymentReference(savedPayment.getPaymentReference())
                .billId(bill.getId())
                .billReference(bill.getBillReference())
                .amountPaid(savedPayment.getAmountPaid())
                .paymentMethod(savedPayment.getPaymentMethod())
                .paymentDate(savedPayment.getPaymentDate())
                .processedByName(processedBy != null ? processedBy.getFullNames() : null)
                .outstandingBalance(refreshed.getOutstandingBalance())
                .createdAt(savedPayment.getCreatedAt())
                .build();
    }

    public List<PaymentResponse> getPaymentsByBill(Long billId) {
        if (!billRepository.existsById(billId)) {
            throw new ResourceNotFoundException("Bill not found with id: " + billId);
        }
        return paymentRepository.findByBillIdOrderByPaymentDateDesc(billId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<PaymentResponse> getPaymentHistoryByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private String generatePaymentReference() {
        return "PAY-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .billId(payment.getBill().getId())
                .billReference(payment.getBill().getBillReference())
                .amountPaid(payment.getAmountPaid())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .processedByName(payment.getProcessedBy() != null ? payment.getProcessedBy().getFullNames() : null)
                .outstandingBalance(payment.getBill().getOutstandingBalance())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
