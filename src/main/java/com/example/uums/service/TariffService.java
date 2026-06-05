package com.example.uums.service;

import com.example.uums.dto.request.CreatePenaltyRequest;
import com.example.uums.dto.request.CreateServiceChargeRequest;
import com.example.uums.dto.request.CreateTariffRequest;
import com.example.uums.dto.request.CreateTaxRequest;
import com.example.uums.dto.response.TariffResponse;
import com.example.uums.entity.*;
import com.example.uums.enums.TariffType;
import com.example.uums.exception.BusinessRuleException;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;
    private final ServiceChargeRepository serviceChargeRepository;
    private final TaxRepository taxRepository;
    private final PenaltyRepository penaltyRepository;
    private final UserRepository userRepository;

    @Transactional
    public TariffResponse createTariff(CreateTariffRequest request) {
        // Validate
        if (request.getTariffType() == TariffType.FLAT && request.getFlatRate() == null) {
            throw new BusinessRuleException("Flat rate is required for FLAT tariff type.");
        }
        if (request.getTariffType() == TariffType.TIER_BASED &&
            (request.getTiers() == null || request.getTiers().isEmpty())) {
            throw new BusinessRuleException("Tiers are required for TIER_BASED tariff type.");
        }

        // Deactivate previous tariffs of same meter type
        List<Tariff> existingTariffs = tariffRepository.findByMeterTypeOrderByVersionDesc(request.getMeterType());
        for (Tariff existing : existingTariffs) {
            if (Boolean.TRUE.equals(existing.getIsActive())) {
                existing.setIsActive(false);
                existing.setEndDate(request.getEffectiveDate().minusDays(1));
                tariffRepository.save(existing);
            }
        }

        int newVersion = tariffRepository.findMaxVersionByMeterType(request.getMeterType()) + 1;

        User creator = getCurrentUser();

        Tariff tariff = Tariff.builder()
                .name(request.getName())
                .meterType(request.getMeterType())
                .tariffType(request.getTariffType())
                .flatRate(request.getFlatRate())
                .version(newVersion)
                .effectiveDate(request.getEffectiveDate())
                .isActive(true)
                .createdBy(creator)
                .build();

        if (request.getTariffType() == TariffType.TIER_BASED && request.getTiers() != null) {
            List<TariffTier> tiers = request.getTiers().stream()
                    .map(t -> TariffTier.builder()
                            .tariff(tariff)
                            .minConsumption(t.getMinConsumption())
                            .maxConsumption(t.getMaxConsumption())
                            .rate(t.getRate())
                            .tierOrder(t.getTierOrder())
                            .build())
                    .toList();
            tariff.getTiers().addAll(tiers);
        }

        return mapToResponse(tariffRepository.save(tariff));
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> getAllTariffs() {
        return tariffRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TariffResponse getTariffById(Long id) {
        return mapToResponse(tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found with id: " + id)));
    }

    @Transactional
    public ServiceCharge createServiceCharge(CreateServiceChargeRequest request) {
        return serviceChargeRepository.save(ServiceCharge.builder()
                .name(request.getName())
                .meterType(request.getMeterType())
                .amount(request.getAmount())
                .isActive(true)
                .effectiveDate(request.getEffectiveDate())
                .build());
    }

    public List<ServiceCharge> getAllServiceCharges() {
        return serviceChargeRepository.findAll();
    }

    @Transactional
    public Tax createTax(CreateTaxRequest request) {
        return taxRepository.save(Tax.builder()
                .name(request.getName())
                .rate(request.getRate())
                .isActive(true)
                .effectiveDate(request.getEffectiveDate())
                .build());
    }

    public List<Tax> getAllTaxes() {
        return taxRepository.findAll();
    }

    @Transactional
    public Penalty createPenalty(CreatePenaltyRequest request) {
        return penaltyRepository.save(Penalty.builder()
                .name(request.getName())
                .rate(request.getRate())
                .gracePeriodDays(request.getGracePeriodDays() != null ? request.getGracePeriodDays() : 30)
                .isActive(true)
                .effectiveDate(request.getEffectiveDate())
                .build());
    }

    public List<Penalty> getAllPenalties() {
        return penaltyRepository.findAll();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private TariffResponse mapToResponse(Tariff tariff) {
        List<TariffResponse.TariffTierResponse> tierResponses = tariff.getTiers().stream()
                .map(t -> TariffResponse.TariffTierResponse.builder()
                        .id(t.getId())
                        .minConsumption(t.getMinConsumption())
                        .maxConsumption(t.getMaxConsumption())
                        .rate(t.getRate())
                        .tierOrder(t.getTierOrder())
                        .build())
                .toList();

        return TariffResponse.builder()
                .id(tariff.getId())
                .name(tariff.getName())
                .meterType(tariff.getMeterType())
                .tariffType(tariff.getTariffType())
                .flatRate(tariff.getFlatRate())
                .version(tariff.getVersion())
                .effectiveDate(tariff.getEffectiveDate())
                .endDate(tariff.getEndDate())
                .isActive(tariff.getIsActive())
                .tiers(tierResponses)
                .createdAt(tariff.getCreatedAt())
                .build();
    }
}
