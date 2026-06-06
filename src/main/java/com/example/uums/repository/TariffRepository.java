package com.example.uums.repository;

/**
 * Spring Data JPA repository for Tariff entities with eager-loaded tariff tiers.
 * Finds active tariff by meter type and date, lists tariffs by meter type, and max version.
 */
import com.example.uums.entity.Tariff;
import com.example.uums.enums.MeterType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    @Override
    @EntityGraph(attributePaths = "tiers")
    List<Tariff> findAll();

    @Override
    @EntityGraph(attributePaths = "tiers")
    Optional<Tariff> findById(Long id);

    @EntityGraph(attributePaths = "tiers")
    @Query("SELECT t FROM Tariff t WHERE t.meterType = :meterType " +
           "AND t.effectiveDate <= :date " +
           "AND (t.endDate IS NULL OR t.endDate >= :date) " +
           "AND t.isActive = true " +
           "ORDER BY t.version DESC LIMIT 1")
    Optional<Tariff> findActiveTariffForMeterType(
            @Param("meterType") MeterType meterType,
            @Param("date") LocalDate date);

    @EntityGraph(attributePaths = "tiers")
    List<Tariff> findByMeterTypeOrderByVersionDesc(MeterType meterType);

    @Query("SELECT COALESCE(MAX(t.version), 0) FROM Tariff t WHERE t.meterType = :meterType")
    Integer findMaxVersionByMeterType(@Param("meterType") MeterType meterType);
}
