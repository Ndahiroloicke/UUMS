package com.example.uums.repository;

import com.example.uums.entity.ServiceCharge;
import com.example.uums.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceChargeRepository extends JpaRepository<ServiceCharge, Long> {

    @Query("SELECT sc FROM ServiceCharge sc WHERE sc.meterType = :meterType " +
           "AND sc.isActive = true AND sc.effectiveDate <= :date " +
           "ORDER BY sc.effectiveDate DESC LIMIT 1")
    Optional<ServiceCharge> findActiveByMeterType(
            @Param("meterType") MeterType meterType,
            @Param("date") LocalDate date);

    List<ServiceCharge> findByMeterTypeAndIsActiveTrue(MeterType meterType);
}
