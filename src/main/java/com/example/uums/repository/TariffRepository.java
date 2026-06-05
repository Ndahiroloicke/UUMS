package com.example.uums.repository;

import com.example.uums.entity.Tariff;
import com.example.uums.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    @Query("SELECT t FROM Tariff t WHERE t.meterType = :meterType " +
           "AND t.effectiveDate <= :date " +
           "AND (t.endDate IS NULL OR t.endDate >= :date) " +
           "AND t.isActive = true " +
           "ORDER BY t.version DESC LIMIT 1")
    Optional<Tariff> findActiveTariffForMeterType(
            @Param("meterType") MeterType meterType,
            @Param("date") LocalDate date);

    List<Tariff> findByMeterTypeOrderByVersionDesc(MeterType meterType);

    @Query("SELECT COALESCE(MAX(t.version), 0) FROM Tariff t WHERE t.meterType = :meterType")
    Integer findMaxVersionByMeterType(@Param("meterType") MeterType meterType);
}
