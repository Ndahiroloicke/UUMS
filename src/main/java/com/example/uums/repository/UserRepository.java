package com.example.uums.repository;

/**
 * Spring Data JPA repository for User login accounts.
 * Lookup by email, email existence check, and filtering users by role.
 */
import com.example.uums.entity.User;
import com.example.uums.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
}
