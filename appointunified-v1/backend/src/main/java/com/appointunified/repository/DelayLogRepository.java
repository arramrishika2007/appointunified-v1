package com.appointunified.repository;
import com.appointunified.entity.DelayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DelayLogRepository extends JpaRepository<DelayLog, UUID> {
    List<DelayLog> findByProfessionalIdOrderByCreatedAtDesc(UUID professionalId);
    @Query("SELECT COALESCE(SUM(d.delayMinutes),0) FROM DelayLog d WHERE d.professional.id = :profId AND d.createdAt > :since")
    Integer sumDelaysSince(UUID profId, OffsetDateTime since);
}
