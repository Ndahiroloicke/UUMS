package com.example.uums.repository;

import com.example.uums.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"bill", "processedBy"})
    List<Payment> findByBillIdOrderByPaymentDateDesc(Long billId);

    @EntityGraph(attributePaths = {"bill", "processedBy"})
    @Query("SELECT p FROM Payment p WHERE p.bill.customer.id = :customerId ORDER BY p.paymentDate DESC")
    List<Payment> findByCustomerId(@Param("customerId") Long customerId);
}
