package com.appointunified.repository;

import com.appointunified.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProfessionalIdAndVisibleTrueOrderByCreatedAtDesc(
            UUID professionalId, Pageable pageable);

    boolean existsByAppointmentId(UUID appointmentId);

    Optional<Review> findByAppointmentId(UUID appointmentId);

    Page<Review> findByReviewerIdOrderByCreatedAtDesc(UUID reviewerId, Pageable pageable);

    long countByReviewerId(UUID reviewerId);
}
