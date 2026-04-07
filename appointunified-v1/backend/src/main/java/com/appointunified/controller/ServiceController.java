package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.ServiceResponse;
import com.appointunified.entity.Professional;
import com.appointunified.entity.Service;
import com.appointunified.enums.Sector;
import com.appointunified.exception.AppException;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.ServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Professional service catalogue management")
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;

    @GetMapping("/professional/{professionalId}")
    @Operation(summary = "List services for a professional")
    public ResponseEntity<ApiResponse<List<ServiceResponse.Summary>>> listForProfessional(
            @PathVariable UUID professionalId) {
        return ResponseEntity.ok(ApiResponse.ok(
            serviceRepository.findByProfessionalIdAndActiveTrue(professionalId)
                .stream().map(this::toResponse).collect(Collectors.toList())));
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Add a service to your catalogue")
    public ResponseEntity<ApiResponse<ServiceResponse.Summary>> createService(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateServiceRequest request) {

        Professional professional = professionalRepository.findByUserId(userId)
            .orElseThrow(() -> AppException.notFound("Professional profile not found"));

        Service service = new Service();
        service.setProfessional(professional);
        service.setSector(professional.getSector());
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        service.setRequiresDocuments(request.isRequiresDocuments());
        service.setVirtual(request.isVirtual());
        service.setMaxConcurrent((short) 1);
        service.setActive(true);
        service.setCreatedAt(OffsetDateTime.now());
        service.setUpdatedAt(OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(toResponse(serviceRepository.save(service))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Update a service")
    public ResponseEntity<ApiResponse<ServiceResponse.Summary>> updateService(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @RequestBody UpdateServiceRequest request) {

        Service service = serviceRepository.findById(id)
            .orElseThrow(() -> AppException.notFound("Service not found"));

        if (!service.getProfessional().getUser().getId().equals(userId)) {
            throw AppException.forbidden("Not your service");
        }

        if (request.getName() != null)            service.setName(request.getName());
        if (request.getDescription() != null)     service.setDescription(request.getDescription());
        if (request.getDurationMinutes() != null) service.setDurationMinutes(request.getDurationMinutes());
        if (request.getPrice() != null)           service.setPrice(request.getPrice());
        if (request.getActive() != null)          service.setActive(request.getActive());

        return ResponseEntity.ok(ApiResponse.ok(toResponse(serviceRepository.save(service))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Deactivate a service")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        Service service = serviceRepository.findById(id)
            .orElseThrow(() -> AppException.notFound("Service not found"));

        if (!service.getProfessional().getUser().getId().equals(userId)) {
            throw AppException.forbidden("Not your service");
        }

        service.setActive(false);
        serviceRepository.save(service);
        return ResponseEntity.ok(ApiResponse.ok("Service deactivated", null));
    }

    private ServiceResponse.Summary toResponse(Service s) {
        ServiceResponse.Summary summary = new ServiceResponse.Summary();
        summary.setId(s.getId());
        summary.setName(s.getName());
        summary.setDescription(s.getDescription());
        summary.setDurationMinutes(s.getDurationMinutes());
        summary.setPrice(s.getPrice());
        summary.setIsActive(s.isActive());
        summary.setRequiresDocuments(s.isRequiresDocuments());
        summary.setIsVirtual(s.isVirtual());
        return summary;
    }

    // ─── Inner request DTOs ─────────────────────────────────────────────────

    public static class CreateServiceRequest {
        @NotBlank @Size(max = 255)
        private String name;
        private String description;
        @NotNull @Min(5) @Max(480)
        private Integer durationMinutes;
        @DecimalMin("0.0")
        private BigDecimal price;
        private boolean requiresDocuments = false;
        private boolean virtual = false;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public boolean isRequiresDocuments() { return requiresDocuments; }
        public void setRequiresDocuments(boolean requiresDocuments) { this.requiresDocuments = requiresDocuments; }
        public boolean isVirtual() { return virtual; }
        public void setVirtual(boolean virtual) { this.virtual = virtual; }
    }

    public static class UpdateServiceRequest {
        private String name;
        private String description;
        private Integer durationMinutes;
        private BigDecimal price;
        private Boolean active;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
}
