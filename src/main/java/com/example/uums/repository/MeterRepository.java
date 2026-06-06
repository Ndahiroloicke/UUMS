package com.example.uums.repository;

/**
 * Spring Data JPA repository for Meter entities with eager-loaded customer.
 * Supports lookup by meter number, customer, status, and meter type.
 */
import com.example.uums.entity.Meter;
import com.example.uums.enums.MeterStatus;
import com.example.uums.enums.MeterType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsByMeterNumber(String meterNumber);

    Optional<Meter> findByMeterNumber(String meterNumber);

    @Override
    @EntityGraph(attributePaths = "customer")
    List<Meter> findAll();

    @Override
    @EntityGraph(attributePaths = "customer")
    Optional<Meter> findById(Long id);

    @EntityGraph(attributePaths = "customer")
    List<Meter> findByCustomerId(Long customerId);

    List<Meter> findByCustomerIdAndStatus(Long customerId, MeterStatus status);

    List<Meter> findByMeterTypeAndStatus(MeterType meterType, MeterStatus status);
}
