package com.example.uums.repository;

/**
 * Spring Data JPA repository for MeterReading entities with eager-loaded meter and capturer.
 * Queries readings by meter (latest, by year/month) ordered by reading date.
 */
import com.example.uums.entity.MeterReading;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    @EntityGraph(attributePaths = {"meter", "capturedBy"})
    List<MeterReading> findByMeterIdOrderByReadingDateDesc(Long meterId);

    @Override
    @EntityGraph(attributePaths = {"meter", "capturedBy"})
    Optional<MeterReading> findById(Long id);

    @Query("SELECT mr FROM MeterReading mr WHERE mr.meter.id = :meterId " +
           "AND YEAR(mr.readingDate) = :year AND MONTH(mr.readingDate) = :month")
    Optional<MeterReading> findByMeterAndYearMonth(
            @Param("meterId") Long meterId,
            @Param("year") int year,
            @Param("month") int month);

    @Query("SELECT mr FROM MeterReading mr WHERE mr.meter.id = :meterId " +
           "ORDER BY mr.readingDate DESC LIMIT 1")
    Optional<MeterReading> findLatestByMeterId(@Param("meterId") Long meterId);
}
