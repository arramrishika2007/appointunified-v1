package com.appointunified.repository;
import com.appointunified.entity.QueuePauseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueuePauseLogRepository extends JpaRepository<QueuePauseLog, UUID> {
    Optional<QueuePauseLog> findFirstByProfessionalIdAndResumedAtIsNullOrderByPausedAtDesc(UUID profId);
    List<QueuePauseLog> findByProfessionalIdOrderByPausedAtDesc(UUID profId);
}
