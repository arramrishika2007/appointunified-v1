package com.appointunified.repository;
import com.appointunified.entity.QueueEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface QueueEventRepository extends JpaRepository<QueueEvent, UUID> {
    Page<QueueEvent> findByProfessionalIdOrderByCreatedAtDesc(UUID professionalId, Pageable pageable);
}
