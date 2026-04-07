package com.appointunified.repository;

import com.appointunified.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    Page<Complaint> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    long countByProfessionalIdAndStatusIn(UUID professionalId, java.util.Collection<String> statuses);
}
