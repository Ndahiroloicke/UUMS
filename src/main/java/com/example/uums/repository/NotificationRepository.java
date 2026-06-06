package com.example.uums.repository;

/**
 * Spring Data JPA repository for Notification entities with eager-loaded customer.
 * Lists notifications by customer (all or unread only) and counts unread notifications.
 */
import com.example.uums.entity.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "customer")
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = "customer")
    List<Notification> findByCustomerIdAndIsReadFalse(Long customerId);

    long countByCustomerIdAndIsReadFalse(Long customerId);
}
