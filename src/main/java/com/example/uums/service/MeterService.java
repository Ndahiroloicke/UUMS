package com.example.uums.service;

import com.example.uums.dto.request.CreateMeterRequest;
import com.example.uums.dto.response.MeterResponse;
import com.example.uums.entity.Customer;
import com.example.uums.entity.Meter;
import com.example.uums.enums.MeterStatus;
import com.example.uums.exception.DuplicateResourceException;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.CustomerRepository;
import com.example.uums.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository meterRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public MeterResponse createMeter(CreateMeterRequest request) {
        if (meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new DuplicateResourceException("Meter number already exists: " + request.getMeterNumber());
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Meter meter = Meter.builder()
                .meterNumber(request.getMeterNumber())
                .meterType(request.getMeterType())
                .installationDate(request.getInstallationDate())
                .status(MeterStatus.ACTIVE)
                .customer(customer)
                .build();

        return mapToResponse(meterRepository.save(meter));
    }

    public List<MeterResponse> getMetersByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        return meterRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<MeterResponse> getAllMeters() {
        return meterRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public MeterResponse getMeterById(Long id) {
        return mapToResponse(findById(id));
    }

    @Transactional
    public MeterResponse updateMeterStatus(Long id, MeterStatus status) {
        Meter meter = findById(id);
        meter.setStatus(status);
        return mapToResponse(meterRepository.save(meter));
    }

    public Meter findEntityById(Long id) {
        return findById(id);
    }

    private Meter findById(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found with id: " + id));
    }

    private MeterResponse mapToResponse(Meter meter) {
        return MeterResponse.builder()
                .id(meter.getId())
                .meterNumber(meter.getMeterNumber())
                .meterType(meter.getMeterType())
                .installationDate(meter.getInstallationDate())
                .status(meter.getStatus())
                .customerId(meter.getCustomer().getId())
                .customerName(meter.getCustomer().getFullNames())
                .createdAt(meter.getCreatedAt())
                .build();
    }
}
