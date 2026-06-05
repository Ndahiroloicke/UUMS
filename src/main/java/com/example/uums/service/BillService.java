package com.example.uums.service;

import com.example.uums.dto.request.GenerateBillRequest;
import com.example.uums.dto.response.BillResponse;
import com.example.uums.dto.response.PenaltyApplicationResponse;
import com.example.uums.entity.*;
import com.example.uums.enums.BillStatus;
import com.example.uums.enums.CustomerStatus;
import com.example.uums.enums.MeterStatus;
import com.example.uums.enums.TariffType;
import com.example.uums.enums.UserRole;
import com.example.uums.exception.BusinessRuleException;
import com.example.uums.exception.DuplicateResourceException;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillRepository billRepository;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final TariffRepository tariffRepository;
    private final ServiceChargeRepository serviceChargeRepository;
    private final TaxRepository taxRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EntityManager entityManager;
    private final NotificationService notificationService;

    @Transactional
    public BillResponse generateBill(GenerateBillRequest request) {
        Meter meter = meterRepository.findById(request.getMeterId())
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + request.getMeterId()));

        // Business Rule: Meter must be active
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BusinessRuleException("Meter must be active to generate a bill.");
        }

        // Business Rule: Inactive customers cannot receive bills
        Customer customer = meter.getCustomer();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Inactive customers cannot receive bills.");
        }

        // Validate no duplicate bill for same meter/period
        billRepository.findByMeterAndBillingPeriod(meter.getId(), request.getBillingYear(), request.getBillingMonth())
                .ifPresent(b -> {
                    throw new DuplicateResourceException("Bill already exists for meter " + meter.getMeterNumber() +
                            " for " + request.getBillingMonth() + "/" + request.getBillingYear());
                });

        // Get meter reading for the period
        MeterReading reading = meterReadingRepository
                .findByMeterAndYearMonth(meter.getId(), request.getBillingYear(), request.getBillingMonth())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No reading found for meter " + meter.getMeterNumber() +
                        " for " + request.getBillingMonth() + "/" + request.getBillingYear()));

        // Get active tariff
        Tariff tariff = tariffRepository
                .findActiveTariffForMeterType(meter.getMeterType(), LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active tariff configured for meter type: " + meter.getMeterType()));

        // Calculate amounts
        BigDecimal consumption = reading.getConsumption();
        BigDecimal consumptionAmount = calculateConsumptionAmount(tariff, consumption);

        BigDecimal serviceChargeAmount = serviceChargeRepository
                .findActiveByMeterType(meter.getMeterType(), LocalDate.now())
                .map(ServiceCharge::getAmount)
                .orElse(BigDecimal.ZERO);

        BigDecimal subtotal = consumptionAmount.add(serviceChargeAmount);

        BigDecimal taxRate = taxRepository
                .findActiveTax(LocalDate.now())
                .map(Tax::getRate)
                .orElse(BigDecimal.ZERO);
        BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        YearMonth period = YearMonth.of(request.getBillingYear(), request.getBillingMonth());
        String billReference = generateBillReference(meter.getMeterNumber(), period);

        Bill bill = Bill.builder()
                .billReference(billReference)
                .customer(customer)
                .meter(meter)
                .meterReading(reading)
                .billingPeriod(period.atDay(1))
                .consumption(consumption)
                .consumptionAmount(consumptionAmount)
                .serviceChargeAmount(serviceChargeAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .outstandingBalance(totalAmount)
                .status(BillStatus.PENDING)
                .dueDate(period.atEndOfMonth().plusDays(30))
                .build();

        Bill savedBill = billRepository.save(bill);

        String message = buildBillGeneratedMessage(customer.getFullNames(), period, totalAmount);
        notificationService.saveInAppNotification(customer, message);
        notificationService.sendEmailSilently(customer.getEmail(), "Utility Bill - " + period, message);

        return mapToResponse(savedBill);
    }

    @Transactional
    public BillResponse approveBill(Long billId) {
        Bill bill = findById(billId);

        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING bills can be approved.");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User approver = userRepository.findByEmail(email).orElse(null);

        bill.setStatus(BillStatus.APPROVED);
        bill.setApprovedBy(approver);
        bill.setApprovedAt(java.time.LocalDateTime.now());

        return mapToResponse(billRepository.save(bill));
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByCustomer(Long customerId) {
        validateCustomerBillAccess(customerId);
        return billRepository.findByCustomerIdOrderByBillingPeriodDesc(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getBillsForCurrentUser() {
        Customer customer = resolveCurrentCustomer();
        return billRepository.findByCustomerIdOrderByBillingPeriodDesc(customer.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(Long id) {
        Bill bill = findById(id);
        validateCustomerBillAccess(bill.getCustomer().getId());
        return mapToResponse(bill);
    }

    @Transactional(readOnly = true)
    public BillResponse getBillByReference(String reference) {
        Bill bill = billRepository.findByBillReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + reference));
        validateCustomerBillAccess(bill.getCustomer().getId());
        return mapToResponse(bill);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getAllBills() {
        return billRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Bill findEntityById(Long id) {
        return findById(id);
    }

    public Bill findEntityByReference(String reference) {
        return billRepository.findByBillReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + reference));
    }

    /** Scheduled daily at 01:00 AM — calls stored procedure to apply overdue penalties */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void applyOverduePenaltiesScheduled() {
        PenaltyApplicationResponse result = applyOverduePenalties();
        log.info("Scheduled penalty run: {} bill(s) penalized — {}", result.getBillsPenalized(), result.getDetails());
    }

    @Transactional
    public PenaltyApplicationResponse applyOverduePenalties() {
        List<Long> eligibleBillIds = billRepository.findBillIdsEligibleForPenalty();

        int billsPenalized = ((Number) entityManager
                .createNativeQuery("SELECT apply_overdue_penalties()")
                .getSingleResult()).intValue();

        if (billsPenalized > 0) {
            eligibleBillIds.forEach(this::sendPenaltyEmailNotification);
        }

        String details = billsPenalized > 0
                ? billsPenalized + " bill(s) penalized. penalty_amount, total_amount, and outstanding_balance were increased. Customers were notified in-app and by email."
                : "No bills penalized. Eligible bills must be unpaid, have outstanding balance > 0, and be past due_date + grace_period_days (30 days after due date by default).";

        return PenaltyApplicationResponse.builder()
                .billsPenalized(billsPenalized)
                .details(details)
                .build();
    }

    private void sendPenaltyEmailNotification(Long billId) {
        billRepository.findById(billId).ifPresent(bill -> {
            String period = bill.getBillingPeriod().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            String message = "Dear " + bill.getCustomer().getFullNames() + ",\nYour " + period +
                    " utility bill (" + bill.getBillReference() + ") is OVERDUE.\n" +
                    "A late payment penalty has been applied.\n" +
                    "New outstanding balance: " + bill.getOutstandingBalance() + " FRW.\n" +
                    "Please pay immediately to avoid further charges.";
            notificationService.sendEmailSilently(
                    bill.getCustomer().getEmail(),
                    "Late Payment Penalty - " + bill.getBillReference(),
                    message);
        });
    }

    private Customer resolveCurrentCustomer() {
        User user = getCurrentUser();
        if (user == null) {
            throw new BusinessRuleException("Authenticated user not found");
        }
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "No customer profile linked to your account. Contact admin to link your user to a customer record."));
    }

    private void validateCustomerBillAccess(Long customerId) {
        User user = getCurrentUser();
        if (user == null || user.getRole() == UserRole.ROLE_ADMIN
                || user.getRole() == UserRole.ROLE_FINANCE) {
            return;
        }
        if (user.getRole() == UserRole.ROLE_CUSTOMER) {
            Customer customer = customerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "No customer profile linked to your account."));
            if (!customer.getId().equals(customerId)) {
                throw new AccessDeniedException("You can only view your own bills");
            }
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private BigDecimal calculateConsumptionAmount(Tariff tariff, BigDecimal consumption) {
        if (tariff.getTariffType() == TariffType.FLAT) {
            return consumption.multiply(tariff.getFlatRate()).setScale(2, RoundingMode.HALF_UP);
        }

        // Tier-based calculation
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal remaining = consumption;

        List<TariffTier> tiers = tariff.getTiers().stream()
                .sorted(Comparator.comparingInt(TariffTier::getTierOrder))
                .toList();

        for (TariffTier tier : tiers) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal tierMin = tier.getMinConsumption();
            BigDecimal tierMax = tier.getMaxConsumption();

            BigDecimal tierCapacity = (tierMax != null)
                    ? tierMax.subtract(tierMin)
                    : remaining;

            BigDecimal unitsInTier = remaining.min(tierCapacity);
            total = total.add(unitsInTier.multiply(tier.getRate()));
            remaining = remaining.subtract(unitsInTier);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateBillReference(String meterNumber, YearMonth period) {
        return String.format("BILL-%d%02d-%s", period.getYear(), period.getMonthValue(), meterNumber);
    }

    static String buildBillGeneratedMessage(String customerName, YearMonth period, BigDecimal amount) {
        return "Dear " + customerName + ",\nYour " +
                period.format(DateTimeFormatter.ofPattern("MMMM yyyy")) +
                " utility bill of " + amount + " FRW has been successfully processed.";
    }

    static String buildBillPaidMessage(String customerName, YearMonth period, BigDecimal amount) {
        return "Dear " + customerName + ",\nYour " +
                period.format(DateTimeFormatter.ofPattern("MMMM yyyy")) +
                " utility bill of " + amount + " FRW has been fully paid. Thank you!";
    }

    private Bill findById(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    private BillResponse mapToResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .billReference(bill.getBillReference())
                .customerId(bill.getCustomer().getId())
                .customerName(bill.getCustomer().getFullNames())
                .meterId(bill.getMeter().getId())
                .meterNumber(bill.getMeter().getMeterNumber())
                .meterType(bill.getMeter().getMeterType().name())
                .billingPeriod(bill.getBillingPeriod())
                .consumption(bill.getConsumption())
                .consumptionAmount(bill.getConsumptionAmount())
                .serviceChargeAmount(bill.getServiceChargeAmount())
                .taxAmount(bill.getTaxAmount())
                .penaltyAmount(bill.getPenaltyAmount())
                .totalAmount(bill.getTotalAmount())
                .amountPaid(bill.getAmountPaid())
                .outstandingBalance(bill.getOutstandingBalance())
                .status(bill.getStatus())
                .dueDate(bill.getDueDate())
                .approvedBy(bill.getApprovedBy() != null ? bill.getApprovedBy().getFullNames() : null)
                .approvedAt(bill.getApprovedAt())
                .createdAt(bill.getCreatedAt())
                .build();
    }
}
