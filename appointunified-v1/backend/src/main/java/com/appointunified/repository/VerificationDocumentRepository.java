package com.appointunified.repository;

import com.appointunified.entity.VerificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, UUID> {
    List<VerificationDocument> findByProfessionalIdOrderBySubmittedAtDesc(UUID professionalId);
    Optional<VerificationDocument> findByDocHash(String docHash);
    Page<VerificationDocument> findByStatusOrderBySubmittedAtAsc(String status, Pageable pageable);
}
