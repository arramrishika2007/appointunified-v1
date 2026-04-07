package com.appointunified.repository;

import com.appointunified.entity.CancellationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CancellationHistoryRepository extends JpaRepository<CancellationHistory, UUID> {
}
