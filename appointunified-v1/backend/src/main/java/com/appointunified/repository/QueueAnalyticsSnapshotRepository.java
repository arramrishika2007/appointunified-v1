package com.appointunified.repository;
import com.appointunified.entity.QueueAnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QueueAnalyticsSnapshotRepository extends JpaRepository<QueueAnalyticsSnapshot, UUID> {
    List<QueueAnalyticsSnapshot> findByProfessionalIdAndSnapshotAtAfterOrderBySnapshotAtDesc(UUID profId, OffsetDateTime since);
}
