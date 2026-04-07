package com.appointunified.repository;

import com.appointunified.entity.ComplaintEscalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintEscalationRepository extends JpaRepository<ComplaintEscalation, UUID> {
}
