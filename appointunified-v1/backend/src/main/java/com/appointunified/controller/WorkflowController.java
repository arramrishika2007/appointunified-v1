package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.WorkflowResponse;
import com.appointunified.service.WorkflowEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/workflows")
@Tag(name = "Workflows", description = "V6 workflow engine")
public class WorkflowController {

    private final WorkflowEngineService workflowEngineService;

    public WorkflowController(WorkflowEngineService workflowEngineService) {
        this.workflowEngineService = workflowEngineService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create workflow definition")
    public ResponseEntity<ApiResponse<WorkflowResponse.DefinitionSummary>> createWorkflow(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(workflowEngineService.createDefinition(
                userId,
                request.getName(),
                request.getSector(),
                request.getDescription(),
                request.getSteps()
        )));
    }

    @GetMapping
    @Operation(summary = "List workflow definitions")
    public ResponseEntity<ApiResponse<List<WorkflowResponse.DefinitionSummary>>> listWorkflows(
            @RequestParam(required = false) String sector) {
        return ResponseEntity.ok(ApiResponse.ok(workflowEngineService.listDefinitions(sector)));
    }

    @PostMapping("/start")
    @Operation(summary = "Start a workflow instance for current user")
    public ResponseEntity<ApiResponse<WorkflowResponse.InstanceSummary>> startWorkflow(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StartWorkflowRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(workflowEngineService.startWorkflow(userId, request.getWorkflowId())));
    }

    @GetMapping("/instances/me")
    @Operation(summary = "Get my workflow instances")
    public ResponseEntity<ApiResponse<List<WorkflowResponse.InstanceSummary>>> getMyInstances(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(workflowEngineService.getMyInstances(userId)));
    }

    @PostMapping("/instances/{id}/complete-step")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Mark current workflow step complete")
    public ResponseEntity<ApiResponse<WorkflowResponse.InstanceSummary>> completeStep(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CompleteStepRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(workflowEngineService.completeStep(id, userId, request.getAppointmentId())));
    }

    public static class CreateWorkflowRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String sector;
        private String description;
        @NotNull
        private List<Map<String, Object>> steps;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Map<String, Object>> getSteps() { return steps; }
        public void setSteps(List<Map<String, Object>> steps) { this.steps = steps; }
    }

    public static class StartWorkflowRequest {
        @NotNull
        private UUID workflowId;

        public UUID getWorkflowId() { return workflowId; }
        public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }
    }

    public static class CompleteStepRequest {
        @NotNull
        private UUID appointmentId;

        public UUID getAppointmentId() { return appointmentId; }
        public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
    }
}
