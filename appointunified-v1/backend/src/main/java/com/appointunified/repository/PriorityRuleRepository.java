package com.appointunified.repository;

import com.appointunified.entity.PriorityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriorityRuleRepository extends JpaRepository<PriorityRule, UUID> {
    Optional<PriorityRule> findByProfessionalIdAndIsActiveTrue(UUID professionalId);
    Optional<PriorityRule> findByProfessionalId(UUID professionalId);
}
