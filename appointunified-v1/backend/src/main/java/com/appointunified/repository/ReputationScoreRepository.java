package com.appointunified.repository;

import com.appointunified.entity.ReputationScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReputationScoreRepository extends JpaRepository<ReputationScore, UUID> {
    Optional<ReputationScore> findByProfessionalId(UUID professionalId);
}
