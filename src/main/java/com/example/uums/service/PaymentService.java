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

        BigDecimal amountToPay = request.getAmountPaid().min(bill.getOutstandingBalance());
        BigDecimal balanceAfterPayment = bill.getOutstandingBalance().subtract(amountToPay).max(BigDecimal.ZERO);

        User processedBy = getCurrentUser();

        Payment payment = Payment.builder()
                .paymentReference(generatePaymentReference())
                .bill(bill)
                .amountPaid(amountToPay)
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(LocalDateTime.now())
                .outstandingBalanceAfterPayment(balanceAfterPayment)
                .processedBy(processedBy)
                .build();

        bill.setAmountPaid(bill.getAmountPaid().add(amountToPay));
        bill.setOutstandingBalance(balanceAfterPayment);

        if (balanceAfterPayment.compareTo(BigDecimal.ZERO) <= 0) {
            bill.setStatus(BillStatus.PAID);
        }

        billRepository.save(bill);
        Payment savedPayment = paymentRepository.save(payment);

        notifyCustomerOnPayment(bill, savedPayment, balanceAfterPayment);

        return mapToResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBill(Long billId) {
        if (!billRepository.existsById(billId)) {
            throw new ResourceNotFoundException("Bill not found with id: " + billId);
        }
        return paymentRepository.findByBillIdOrderByPaymentDateDesc(billId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistoryByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void notifyCustomerOnPayment(Bill bill, Payment payment, BigDecimal balanceAfterPayment) {
        String customerName = bill.getCustomer().getFullNames();
        String customerEmail = bill.getCustomer().getEmail();
        String period = bill.getBillingPeriod().format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        if (balanceAfterPayment.compareTo(BigDecimal.ZERO) <= 0) {
            // In-app notification is created by DB trigger (fn_notify_on_bill_paid)
            String message = "Dear " + customerName + ",\nYour " + period +
                    " utility bill of " + bill.getTotalAmount() +
                    " FRW has been fully paid. Thank you!";
            notificationService.sendEmailSilently(
                    customerEmail,
                    "Payment Confirmation - " + bill.getBillReference(),
                    message);
        } else {
            String message = "Dear " + customerName + ",\nWe received your payment of " +
                    payment.getAmountPaid() + " FRW for bill " + bill.getBillReference() +
                    ". Outstanding balance: " + balanceAfterPayment + " FRW.";
            notificationService.saveInAppNotification(bill.getCustomer(), message);
            notificationService.sendEmailSilently(
                    customerEmail,
                    "Payment Received - " + bill.getBillReference(),
                    message);
        }
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
                .outstandingBalance(payment.getOutstandingBalanceAfterPayment())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
