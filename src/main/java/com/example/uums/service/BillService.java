package com.example.uums.service;

import com.example.uums.dto.request.GenerateBillRequest;
import com.example.uums.dto.response.BillResponse;
import com.example.uums.entity.*;
import com.example.uums.enums.BillStatus;
import com.example.uums.enums.CustomerStatus;
import com.example.uums.enums.MeterStatus;
import com.example.uums.enums.TariffType;
import com.example.uums.exception.BusinessRuleException;
import com.example.uums.exception.DuplicateResourceException;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

        // Send email notification asynchronously
        String message = buildNotificationMessage(customer.getFullNames(), period, totalAmount);
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

    public List<BillResponse> getBillsByCustomer(Long customerId) {
        return billRepository.findByCustomerIdOrderByBillingPeriodDesc(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BillResponse getBillById(Long id) {
        return mapToResponse(findById(id));
    }

    public BillResponse getBillByReference(String reference) {
        return mapToResponse(billRepository.findByBillReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + reference)));
    }

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
    public void applyOverduePenalties() {
        log.info("Running apply_overdue_penalties stored procedure...");
        entityManager.createNativeQuery("CALL apply_overdue_penalties()").executeUpdate();
        log.info("Overdue penalties applied.");
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

    private String buildNotificationMessage(String customerName, YearMonth period, BigDecimal amount) {
        return "Dear " + customerName + ",\nYour " +
                period.format(DateTimeFormatter.ofPattern("MMMM yyyy")) +
                " utility bill of " + amount + " FRW has been successfully processed.";
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
