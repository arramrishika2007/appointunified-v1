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
       @Query("SELECT DISTINCT a FROM Appointment a " +
              "LEFT JOIN FETCH a.client c " +
              "LEFT JOIN FETCH a.professional p " +
              "LEFT JOIN FETCH p.user pu " +
              "LEFT JOIN FETCH a.service s " +
              "WHERE (c.id = :userId OR pu.id = :userId) " +
              "AND a.status NOT IN ('DRAFT', 'CANCELLED', 'EXPIRED') " +
              "ORDER BY a.startTime DESC")
       List<Appointment> findChatAppointmentsForUser(@Param("userId") UUID userId);

       @Query("SELECT a FROM Appointment a WHERE a.client.id = :clientId ORDER BY a.startTime DESC")
       Page<Appointment> findByClientIdOrderByStartTimeDesc(@Param("clientId") UUID clientId, Pageable pageable);

       @Query("SELECT a FROM Appointment a WHERE a.professional.id = :professionalId ORDER BY a.startTime ASC")
       Page<Appointment> findByProfessionalIdOrderByStartTimeAsc(@Param("professionalId") UUID professionalId, Pageable pageable);

    Optional<Appointment> findByShareToken(String shareToken);

       Optional<Appointment> findFirstByMeetingTokenIgnoreCase(String meetingToken);

       long countByProfessional_Id(UUID professionalId);

       long countByProfessional_IdAndStatus(UUID professionalId, AppointmentStatus status);

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

    List<Appointment> findByStatusAndCreatedAtBefore(AppointmentStatus status, OffsetDateTime time);
}
