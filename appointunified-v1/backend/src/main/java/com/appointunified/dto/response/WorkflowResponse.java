package com.appointunified.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkflowResponse {

    public static class DefinitionSummary {
        private UUID id;
        private String name;
        private String sector;
        private String description;
        private List<Map<String, Object>> steps;
        private boolean active;
        private OffsetDateTime createdAt;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Map<String, Object>> getSteps() { return steps; }
        public void setSteps(List<Map<String, Object>> steps) { this.steps = steps; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class StepProgress {
        private int order;
        private String label;
        private UUID serviceId;
        private UUID professionalId;
        private boolean autoBookNext;
        private boolean requiresCompletion;
        private UUID appointmentId;
        private String appointmentStatus;
        private OffsetDateTime completedAt;

        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public UUID getServiceId() { return serviceId; }
        public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
        public UUID getProfessionalId() { return professionalId; }
        public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }
        public boolean isAutoBookNext() { return autoBookNext; }
        public void setAutoBookNext(boolean autoBookNext) { this.autoBookNext = autoBookNext; }
        public boolean isRequiresCompletion() { return requiresCompletion; }
        public void setRequiresCompletion(boolean requiresCompletion) { this.requiresCompletion = requiresCompletion; }
        public UUID getAppointmentId() { return appointmentId; }
        public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
        public String getAppointmentStatus() { return appointmentStatus; }
        public void setAppointmentStatus(String appointmentStatus) { this.appointmentStatus = appointmentStatus; }
        public OffsetDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    }

    public static class InstanceSummary {
        private UUID id;
        private UUID workflowId;
        private String workflowName;
        private String status;
        private int currentStep;
        private int totalSteps;
        private StepProgress nextStep;
        private OffsetDateTime createdAt;
        private List<StepProgress> steps;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getWorkflowId() { return workflowId; }
        public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }
        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getCurrentStep() { return currentStep; }
        public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        public StepProgress getNextStep() { return nextStep; }
        public void setNextStep(StepProgress nextStep) { this.nextStep = nextStep; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public List<StepProgress> getSteps() { return steps; }
        public void setSteps(List<StepProgress> steps) { this.steps = steps; }
    }

    public static class AppointmentWorkflowContext {
        private UUID appointmentId;
        private UUID instanceId;
        private String workflowName;
        private int stepOrder;
        private String stepLabel;
        private String instanceStatus;

        public UUID getAppointmentId() { return appointmentId; }
        public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
        public UUID getInstanceId() { return instanceId; }
        public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }
        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
        public int getStepOrder() { return stepOrder; }
        public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
        public String getStepLabel() { return stepLabel; }
        public void setStepLabel(String stepLabel) { this.stepLabel = stepLabel; }
        public String getInstanceStatus() { return instanceStatus; }
        public void setInstanceStatus(String instanceStatus) { this.instanceStatus = instanceStatus; }
    }
}
