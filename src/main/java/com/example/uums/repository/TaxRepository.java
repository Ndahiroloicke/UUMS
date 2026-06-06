package com.example.uums.repository;

/**
 * Spring Data JPA repository for Tax configuration entities.
 * Finds the active tax for a given date and lists all currently active tax records.
 */
import com.example.uums.entity.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRepository extends JpaRepository<Tax, Long> {

    @Query("SELECT t FROM Tax t WHERE t.isActive = true AND t.effectiveDate <= :date ORDER BY t.effectiveDate DESC LIMIT 1")
    Optional<Tax> findActiveTax(@Param("date") LocalDate date);

    @Query("SELECT t FROM Tax t WHERE t.isActive = true AND t.effectiveDate <= :date")
    List<Tax> findAllActiveTaxes(@Param("date") LocalDate date);

    List<Tax> findByIsActiveTrue();
}
