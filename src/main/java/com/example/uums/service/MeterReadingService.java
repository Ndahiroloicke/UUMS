package com.example.uums.service;

/**
 * Business logic for capturing and retrieving meter consumption readings.
 * Enforces active meters, one reading per month, monotonically increasing values,
 * auto-calculates consumption, and records which user captured each reading.
 */
import com.example.uums.dto.request.CaptureMeterReadingRequest;
import com.example.uums.dto.response.MeterReadingResponse;
import com.example.uums.entity.Meter;
import com.example.uums.entity.MeterReading;
import com.example.uums.entity.User;
import com.example.uums.enums.MeterStatus;
import com.example.uums.exception.BusinessRuleException;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.MeterReadingRepository;
import com.example.uums.repository.MeterRepository;
import com.example.uums.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;
    private final UserRepository userRepository;

    @Transactional
    public MeterReadingResponse captureReading(CaptureMeterReadingRequest request) {
        Meter meter = meterRepository.findById(request.getMeterId())
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + request.getMeterId()));

        // Business Rule: Meter must be active
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot capture reading for an inactive meter.");
        }

        // Business Rule: Only one reading per meter per month/year
        int year = request.getReadingDate().getYear();
        int month = request.getReadingDate().getMonthValue();
        Optional<MeterReading> existing = meterReadingRepository.findByMeterAndYearMonth(meter.getId(), year, month);
        if (existing.isPresent()) {
            throw new BusinessRuleException(
                    "A reading already exists for meter " + meter.getMeterNumber() +
                    " for " + month + "/" + year);
        }

        // Determine previous reading
        BigDecimal previousReading = meterReadingRepository
                .findLatestByMeterId(meter.getId())
                .map(MeterReading::getCurrentReading)
                .orElse(BigDecimal.ZERO);

        // Business Rule: Current reading must be greater than previous reading
        if (request.getCurrentReading().compareTo(previousReading) <= 0) {
            throw new BusinessRuleException(
                    "Current reading (" + request.getCurrentReading() +
                    ") must be greater than previous reading (" + previousReading + ").");
        }

        // Capture reading
        User capturedBy = getCurrentUser();
        BigDecimal consumption = request.getCurrentReading().subtract(previousReading);

        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .previousReading(previousReading)
                .currentReading(request.getCurrentReading())
                .consumption(consumption)
                .readingDate(request.getReadingDate())
                .capturedBy(capturedBy)
                .build();

        return mapToResponse(meterReadingRepository.save(reading));
    }

    @Transactional(readOnly = true)
    public List<MeterReadingResponse> getReadingsByMeter(Long meterId) {
        if (!meterRepository.existsById(meterId)) {
            throw new ResourceNotFoundException("Meter not found with id: " + meterId);
        }
        return meterReadingRepository.findByMeterIdOrderByReadingDateDesc(meterId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MeterReadingResponse getReadingById(Long id) {
        MeterReading reading = meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
        return mapToResponse(reading);
    }

    public MeterReading findEntityByMeterAndPeriod(Long meterId, int year, int month) {
        return meterReadingRepository.findByMeterAndYearMonth(meterId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No reading found for meter ID " + meterId + " for " + month + "/" + year));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private MeterReadingResponse mapToResponse(MeterReading reading) {
        return MeterReadingResponse.builder()
                .id(reading.getId())
                .meterId(reading.getMeter().getId())
                .meterNumber(reading.getMeter().getMeterNumber())
                .previousReading(reading.getPreviousReading())
                .currentReading(reading.getCurrentReading())
                .consumption(reading.getConsumption())
                .readingDate(reading.getReadingDate())
                .capturedByName(reading.getCapturedBy() != null ? reading.getCapturedBy().getFullNames() : null)
                .createdAt(reading.getCreatedAt())
                .build();
    }
}
