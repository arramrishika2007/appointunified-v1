package com.appointunified.repository;

import com.appointunified.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {
    List<WorkflowDefinition> findByActiveTrueOrderByCreatedAtDesc();
    List<WorkflowDefinition> findByActiveTrueAndSectorIgnoreCaseOrderByCreatedAtDesc(String sector);
}
