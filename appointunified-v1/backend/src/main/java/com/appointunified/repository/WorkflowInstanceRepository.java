package com.appointunified.repository;

import com.appointunified.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    @EntityGraph(attributePaths = {"workflow"})
    List<WorkflowInstance> findByUserIdAndStatusInOrderByCreatedAtDesc(UUID userId, List<String> statuses);

    @EntityGraph(attributePaths = {"workflow"})
    Optional<WorkflowInstance> findWithWorkflowById(UUID id);
}
