package com.example.uums.repository;

import com.example.uums.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    List<MeterReading> findByMeterIdOrderByReadingDateDesc(Long meterId);

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
