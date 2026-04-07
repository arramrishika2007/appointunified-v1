package com.appointunified.repository;

import com.appointunified.entity.BehaviorScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BehaviorScoreRepository extends JpaRepository<BehaviorScore, UUID> {
    Optional<BehaviorScore> findByUserId(UUID userId);
}
