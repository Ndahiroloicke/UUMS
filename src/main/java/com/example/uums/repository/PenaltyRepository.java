package com.example.uums.repository;

import com.example.uums.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    @Query("SELECT p FROM Penalty p WHERE p.isActive = true AND p.effectiveDate <= :date ORDER BY p.effectiveDate DESC LIMIT 1")
    Optional<Penalty> findActivePenalty(@Param("date") LocalDate date);

    List<Penalty> findByIsActiveTrue();
}
