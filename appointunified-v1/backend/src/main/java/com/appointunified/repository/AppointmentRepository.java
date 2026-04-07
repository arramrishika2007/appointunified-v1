package com.appointunified.repository;

import com.appointunified.entity.Appointment;
import com.appointunified.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Page<Appointment> findByClientIdOrderByStartTimeDesc(UUID clientId, Pageable pageable);
    Page<Appointment> findByProfessionalIdOrderByStartTimeAsc(UUID professionalId, Pageable pageable);
    Optional<Appointment> findByShareToken(String shareToken);
       long countByProfessionalId(UUID professionalId);
       long countByProfessionalIdAndStatus(UUID professionalId, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.professional.id = :profId " +
           "AND a.startTime BETWEEN :from AND :to " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW', 'EXPIRED')")
    List<Appointment> findBookedSlots(@Param("profId") UUID professionalId,
                                       @Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.professional.id = :profId " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW', 'EXPIRED') " +
           "AND a.startTime < :end AND a.endTime > :start")
    boolean hasConflict(@Param("profId") UUID professionalId,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end);
}
