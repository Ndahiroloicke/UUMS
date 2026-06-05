package com.example.uums.repository;

import com.example.uums.entity.TariffTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TariffTierRepository extends JpaRepository<TariffTier, Long> {
    List<TariffTier> findByTariffIdOrderByTierOrder(Long tariffId);
}
