package com.example.uums.repository;

import com.example.uums.entity.Customer;
import com.example.uums.entity.User;
import com.example.uums.enums.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByNationalId(String nationalId);
    boolean existsByEmail(String email);
    Optional<Customer> findByNationalId(String nationalId);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUser(User user);

    Optional<Customer> findByUserId(Long userId);

    List<Customer> findByStatus(CustomerStatus status);
}
