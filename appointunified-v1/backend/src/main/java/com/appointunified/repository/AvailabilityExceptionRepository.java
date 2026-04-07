package com.appointunified.repository;

import com.appointunified.entity.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, UUID> {

    List<AvailabilityException> findByProfessionalIdOrderByExceptionDateAsc(UUID professionalId);

    @Query("SELECT e FROM AvailabilityException e " +
           "WHERE e.professional.id = :professionalId " +
           "AND e.exceptionDate >= :from AND e.exceptionDate <= :to " +
           "ORDER BY e.exceptionDate")
    List<AvailabilityException> findByProfessionalIdAndDateRange(
        @Param("professionalId") UUID professionalId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    boolean existsByProfessionalIdAndExceptionDate(UUID professionalId, LocalDate date);

    /** Used by slot engine: find exception for a specific date */
    java.util.Optional<AvailabilityException> findByProfessionalIdAndExceptionDate(
        UUID professionalId, LocalDate date);
}
