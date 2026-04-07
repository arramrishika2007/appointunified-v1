package com.appointunified.repository;

import com.appointunified.entity.SLAEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SLAEventRepository extends JpaRepository<SLAEvent, UUID> {
    List<SLAEvent> findByAppointmentId(UUID appointmentId);
    
    @Query("SELECT s FROM SLAEvent s WHERE s.resolvedAt IS NULL ORDER BY s.breachedAt DESC")
    List<SLAEvent> findActiveBreaches();
    
    @Query("SELECT s FROM SLAEvent s WHERE s.escalatedTo = :userId AND s.resolvedAt IS NULL")
    List<SLAEvent> findActiveAlertsByEscalatedUser(UUID userId);
    
    List<SLAEvent> findByBreachedAtGreaterThanEqual(LocalDateTime since);
}
