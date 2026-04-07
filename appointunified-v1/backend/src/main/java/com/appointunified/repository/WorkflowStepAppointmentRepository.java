package com.appointunified.repository;

import com.appointunified.entity.WorkflowStepAppointment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStepAppointmentRepository extends JpaRepository<WorkflowStepAppointment, UUID> {

    @EntityGraph(attributePaths = {"appointment", "appointment.professional", "appointment.service"})
    List<WorkflowStepAppointment> findByInstanceIdOrderByStepOrderAsc(UUID instanceId);

    Optional<WorkflowStepAppointment> findByInstanceIdAndStepOrder(UUID instanceId, int stepOrder);

    @EntityGraph(attributePaths = {"instance", "instance.workflow", "appointment", "appointment.client", "appointment.professional"})
    Optional<WorkflowStepAppointment> findByAppointmentId(UUID appointmentId);
}
