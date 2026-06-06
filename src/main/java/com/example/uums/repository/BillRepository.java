package com.example.uums.repository;

/**
 * Spring Data JPA repository for Bill entities with eager-loading entity graphs.
 * Supports lookup by reference, customer, meter, billing period, status, overdue bills,
 * and native SQL query for bills eligible for late-payment penalty application.
 */
import com.example.uums.entity.Bill;
import com.example.uums.enums.BillStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    @EntityGraph(attributePaths = {"customer", "meter", "approvedBy"})
    Optional<Bill> findByBillReference(String billReference);

    @EntityGraph(attributePaths = {"customer", "meter", "approvedBy"})
    List<Bill> findByCustomerIdOrderByBillingPeriodDesc(Long customerId);

    List<Bill> findByMeterIdOrderByBillingPeriodDesc(Long meterId);

    List<Bill> findByStatus(BillStatus status);

    @Override
    @EntityGraph(attributePaths = {"customer", "meter", "approvedBy"})
    List<Bill> findAll();

    @Override
    @EntityGraph(attributePaths = {"customer", "meter", "approvedBy"})
    Optional<Bill> findById(Long id);

    @Query("SELECT b FROM Bill b WHERE b.meter.id = :meterId " +
           "AND YEAR(b.billingPeriod) = :year AND MONTH(b.billingPeriod) = :month")
    Optional<Bill> findByMeterAndBillingPeriod(
            @Param("meterId") Long meterId,
            @Param("year") int year,
            @Param("month") int month);

    @Query("SELECT b FROM Bill b WHERE b.status NOT IN ('PAID') AND b.dueDate < :today")
    List<Bill> findOverdueBills(@Param("today") LocalDate today);

    @Query(value = """
            SELECT b.id FROM bills b
            WHERE b.status <> 'PAID'
              AND b.outstanding_balance > 0
              AND CURRENT_DATE > (b.due_date + (
                  SELECT COALESCE(p.grace_period_days, 0)
                  FROM penalties p
                  WHERE p.is_active = TRUE
                  ORDER BY p.effective_date DESC
                  LIMIT 1
              ) * INTERVAL '1 day')
            """, nativeQuery = true)
    List<Long> findBillIdsEligibleForPenalty();
}
