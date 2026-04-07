package com.appointunified.service;

import com.appointunified.dto.response.WorkflowResponse;
import com.appointunified.entity.*;
import com.appointunified.exception.AppException;
import com.appointunified.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngineService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStepAppointmentRepository workflowStepAppointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final NotificationService notificationService;

    @Transactional
    public WorkflowResponse.DefinitionSummary createDefinition(UUID adminUserId,
                                                              String name,
                                                              String sector,
                                                              String description,
                                                              List<Map<String, Object>> steps) {
        if (steps == null || steps.isEmpty()) {
            throw AppException.badRequest("Workflow must contain at least one step");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> AppException.notFound("Admin user not found"));

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setName(name);
        definition.setSector(sector);
        definition.setDescription(description);
        definition.setSteps(normalizeSteps(steps));
        definition.setActive(true);
        definition.setCreatedBy(admin);
        definition = workflowDefinitionRepository.save(definition);

        return toDefinitionSummary(definition);
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse.DefinitionSummary> listDefinitions(String sector) {
        List<WorkflowDefinition> list;
        if (sector == null || sector.isBlank()) {
            list = workflowDefinitionRepository.findByActiveTrueOrderByCreatedAtDesc();
        } else {
            list = workflowDefinitionRepository.findByActiveTrueAndSectorIgnoreCaseOrderByCreatedAtDesc(sector);
        }
        return list.stream().map(this::toDefinitionSummary).collect(Collectors.toList());
    }

    @Transactional
    public WorkflowResponse.InstanceSummary startWorkflow(UUID userId, UUID workflowId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        WorkflowDefinition definition = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> AppException.notFound("Workflow not found"));

        if (!definition.isActive()) {
            throw AppException.badRequest("Workflow is not active");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflow(definition);
        instance.setUser(user);
        instance.setCurrentStep(1);
        instance.setStatus("IN_PROGRESS");
        instance = workflowInstanceRepository.save(instance);

        return toInstanceSummary(instance,
                workflowStepAppointmentRepository.findByInstanceIdOrderByStepOrderAsc(instance.getId()));
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse.InstanceSummary> getMyInstances(UUID userId) {
        List<WorkflowInstance> instances = workflowInstanceRepository
                .findByUserIdAndStatusInOrderByCreatedAtDesc(userId, List.of("IN_PROGRESS", "COMPLETED"));

        return instances.stream()
                .map(instance -> toInstanceSummary(instance,
                        workflowStepAppointmentRepository.findByInstanceIdOrderByStepOrderAsc(instance.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowResponse.InstanceSummary completeStep(UUID instanceId, UUID professionalUserId, UUID appointmentId) {
        WorkflowInstance instance = workflowInstanceRepository.findWithWorkflowById(instanceId)
                .orElseThrow(() -> AppException.notFound("Workflow instance not found"));

        Professional professional = professionalRepository.findByUserId(professionalUserId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (!appointment.getProfessional().getId().equals(professional.getId())) {
            throw AppException.forbidden("Not your appointment");
        }
        if (!appointment.getClient().getId().equals(instance.getUser().getId())) {
            throw AppException.badRequest("Appointment user does not match workflow instance user");
        }

        int stepOrder = instance.getCurrentStep();
        WorkflowStepAppointment stepAppointment = workflowStepAppointmentRepository
                .findByInstanceIdAndStepOrder(instance.getId(), stepOrder)
                .orElseGet(() -> {
                    WorkflowStepAppointment created = new WorkflowStepAppointment();
                    created.setInstance(instance);
                    created.setStepOrder(stepOrder);
                    created.setAppointment(appointment);
                    return created;
                });

        stepAppointment.setAppointment(appointment);
        stepAppointment.setCompletedAt(OffsetDateTime.now());
        workflowStepAppointmentRepository.save(stepAppointment);

        advanceInstance(instance, appointment);

        return toInstanceSummary(instance,
                workflowStepAppointmentRepository.findByInstanceIdOrderByStepOrderAsc(instance.getId()));
    }

    @Transactional
    public void linkAppointmentToWorkflowStep(UUID userId,
                                              UUID workflowInstanceId,
                                              Integer workflowStepOrder,
                                              Appointment appointment) {
        WorkflowInstance instance = workflowInstanceRepository.findWithWorkflowById(workflowInstanceId)
                .orElseThrow(() -> AppException.notFound("Workflow instance not found"));

        if (!instance.getUser().getId().equals(userId)) {
            throw AppException.forbidden("You do not own this workflow instance");
        }

        int stepOrder = workflowStepOrder != null ? workflowStepOrder : instance.getCurrentStep();
        int totalSteps = instance.getWorkflow().getSteps().size();
        if (stepOrder < 1 || stepOrder > totalSteps) {
            throw AppException.badRequest("Invalid workflow step");
        }

        WorkflowStepAppointment linked = workflowStepAppointmentRepository
                .findByInstanceIdAndStepOrder(workflowInstanceId, stepOrder)
                .orElseGet(() -> {
                    WorkflowStepAppointment created = new WorkflowStepAppointment();
                    created.setInstance(instance);
                    created.setStepOrder(stepOrder);
                    return created;
                });

        if (linked.getAppointment() != null && !linked.getAppointment().getId().equals(appointment.getId())) {
            throw AppException.conflict("Workflow step already linked to another appointment");
        }

        linked.setAppointment(appointment);
        workflowStepAppointmentRepository.save(linked);
    }

    @Transactional
    public void handleAppointmentCompleted(Appointment appointment) {
        workflowStepAppointmentRepository.findByAppointmentId(appointment.getId())
                .ifPresent(stepAppointment -> {
                    WorkflowInstance instance = stepAppointment.getInstance();
                    if (stepAppointment.getCompletedAt() == null) {
                        stepAppointment.setCompletedAt(OffsetDateTime.now());
                        workflowStepAppointmentRepository.save(stepAppointment);
                    }
                    advanceInstance(instance, appointment);
                });
    }

    @Transactional(readOnly = true)
    public WorkflowResponse.AppointmentWorkflowContext getAppointmentWorkflowContext(UUID appointmentId, UUID requesterId) {
        WorkflowStepAppointment stepAppointment = workflowStepAppointmentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> AppException.notFound("No workflow context found for this appointment"));

        Appointment appointment = stepAppointment.getAppointment();
        boolean isClient = appointment.getClient().getId().equals(requesterId);
        boolean isProfessional = appointment.getProfessional().getUser().getId().equals(requesterId);

        if (!isClient && !isProfessional) {
            throw AppException.forbidden("You do not have access to this appointment workflow");
        }

        WorkflowResponse.AppointmentWorkflowContext context = new WorkflowResponse.AppointmentWorkflowContext();
        context.setAppointmentId(appointmentId);
        context.setInstanceId(stepAppointment.getInstance().getId());
        context.setWorkflowName(stepAppointment.getInstance().getWorkflow().getName());
        context.setStepOrder(stepAppointment.getStepOrder());
        context.setStepLabel(findStepLabel(stepAppointment.getInstance().getWorkflow(), stepAppointment.getStepOrder()));
        context.setInstanceStatus(stepAppointment.getInstance().getStatus());
        return context;
    }

    private void advanceInstance(WorkflowInstance instance, Appointment completedAppointment) {
        List<Map<String, Object>> steps = instance.getWorkflow().getSteps();
        int totalSteps = steps.size();
        int current = instance.getCurrentStep();

        Map<String, Object> currentStepDef = findStep(steps, current);
        boolean autoBookNext = toBoolean(currentStepDef.get("auto_book_next"));

        if (current >= totalSteps) {
            instance.setStatus("COMPLETED");
            workflowInstanceRepository.save(instance);
            return;
        }

        instance.setCurrentStep(current + 1);
        instance.setStatus("IN_PROGRESS");
        workflowInstanceRepository.save(instance);

        if (autoBookNext) {
            notificationService.sendWorkflowNextStepPrompt(instance.getUser(), instance.getWorkflow(), current + 1, completedAppointment);
        }
    }

    private List<Map<String, Object>> normalizeSteps(List<Map<String, Object>> steps) {
        return steps.stream()
                .sorted(Comparator.comparingInt(s -> toInt(s.get("order"), 0)))
                .collect(Collectors.toList());
    }

    private WorkflowResponse.DefinitionSummary toDefinitionSummary(WorkflowDefinition definition) {
        WorkflowResponse.DefinitionSummary summary = new WorkflowResponse.DefinitionSummary();
        summary.setId(definition.getId());
        summary.setName(definition.getName());
        summary.setSector(definition.getSector());
        summary.setDescription(definition.getDescription());
        summary.setSteps(definition.getSteps());
        summary.setActive(definition.isActive());
        summary.setCreatedAt(definition.getCreatedAt());
        return summary;
    }

    private WorkflowResponse.InstanceSummary toInstanceSummary(WorkflowInstance instance,
                                                               List<WorkflowStepAppointment> linkedSteps) {
        WorkflowResponse.InstanceSummary summary = new WorkflowResponse.InstanceSummary();
        summary.setId(instance.getId());
        summary.setWorkflowId(instance.getWorkflow().getId());
        summary.setWorkflowName(instance.getWorkflow().getName());
        summary.setStatus(instance.getStatus());
        summary.setCurrentStep(instance.getCurrentStep());
        summary.setCreatedAt(instance.getCreatedAt());

        List<Map<String, Object>> defs = instance.getWorkflow().getSteps();
        summary.setTotalSteps(defs.size());

        Map<Integer, WorkflowStepAppointment> linkedByOrder = linkedSteps.stream()
                .collect(Collectors.toMap(WorkflowStepAppointment::getStepOrder, s -> s, (a, b) -> a));

        List<WorkflowResponse.StepProgress> stepProgress = new ArrayList<>();
        for (Map<String, Object> def : defs) {
            int order = toInt(def.get("order"), 0);
            WorkflowStepAppointment linked = linkedByOrder.get(order);
            WorkflowResponse.StepProgress step = new WorkflowResponse.StepProgress();
            step.setOrder(order);
            step.setLabel(asString(def.get("label")));
            step.setServiceId(asUuid(def.get("service_id")));
            step.setProfessionalId(asUuid(def.get("professional_id")));
            step.setAutoBookNext(toBoolean(def.get("auto_book_next")));
            step.setRequiresCompletion(toBoolean(def.get("requires_completion")));
            if (linked != null) {
                step.setAppointmentId(linked.getAppointment().getId());
                step.setAppointmentStatus(linked.getAppointment().getStatus().name());
                step.setCompletedAt(linked.getCompletedAt());
            }
            stepProgress.add(step);
        }

        summary.setSteps(stepProgress);
        summary.setNextStep(stepProgress.stream()
                .filter(s -> s.getOrder() == instance.getCurrentStep())
                .findFirst().orElse(null));
        return summary;
    }

    private Map<String, Object> findStep(List<Map<String, Object>> steps, int order) {
        return steps.stream()
                .filter(s -> toInt(s.get("order"), -1) == order)
                .findFirst()
                .orElseGet(HashMap::new);
    }

    private String findStepLabel(WorkflowDefinition definition, int order) {
        return definition.getSteps().stream()
                .filter(s -> toInt(s.get("order"), -1) == order)
                .map(s -> asString(s.get("label")))
                .findFirst()
                .orElse("Step " + order);
    }

    private int toInt(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private UUID asUuid(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }
}
