package com.appointunified.repository;

import com.appointunified.entity.SLAConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SLAConfigRepository extends JpaRepository<SLAConfig, UUID> {
    Optional<SLAConfig> findBySectorAndPriorityTier(String sector, String priorityTier);
    List<SLAConfig> findBySector(String sector);
}
