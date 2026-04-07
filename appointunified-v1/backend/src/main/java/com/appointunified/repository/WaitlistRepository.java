package com.appointunified.repository;

import com.appointunified.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {

    @Query("""
            SELECT COUNT(w) > 0
            FROM WaitlistEntry w
            WHERE w.user.id = :userId
              AND w.professional.id = :professionalId
              AND (:serviceId IS NULL OR w.service.id = :serviceId)
              AND w.notified = false
              AND (w.expiresAt IS NULL OR w.expiresAt > :now)
            """)
    boolean existsActiveEntry(@Param("userId") UUID userId,
                              @Param("professionalId") UUID professionalId,
                              @Param("serviceId") UUID serviceId,
                              @Param("now") OffsetDateTime now);

    List<WaitlistEntry> findByUserIdAndNotifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(UUID userId, OffsetDateTime now);

    List<WaitlistEntry> findByProfessionalIdAndNotifiedFalseAndExpiresAtAfterOrderByCreatedAtAsc(UUID professionalId, OffsetDateTime now);
}
