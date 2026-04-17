package com.appointunified.service;

import com.appointunified.entity.Complaint;
import com.appointunified.entity.ComplaintEscalation;
import com.appointunified.entity.Professional;
import com.appointunified.entity.User;
import com.appointunified.enums.VerificationStatus;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.ComplaintEscalationRepository;
import com.appointunified.repository.ComplaintRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintEscalationRepository complaintEscalationRepository;
    private final ProfessionalRepository professionalRepository;
    private final UserRepository userRepository;
        private final AppointmentRepository appointmentRepository;
    private final NotificationServiceV2Extension notificationServiceV2Extension;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintEscalationRepository complaintEscalationRepository,
            ProfessionalRepository professionalRepository,
            UserRepository userRepository,
                        AppointmentRepository appointmentRepository,
            NotificationServiceV2Extension notificationServiceV2Extension) {
        this.complaintRepository = complaintRepository;
        this.complaintEscalationRepository = complaintEscalationRepository;
        this.professionalRepository = professionalRepository;
        this.userRepository = userRepository;
                this.appointmentRepository = appointmentRepository;
        this.notificationServiceV2Extension = notificationServiceV2Extension;
    }

    @Transactional
    public ComplaintView file(UUID reporterId, FileComplaintRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> AppException.notFound("Reporter not found"));
        Professional professional = professionalRepository.findById(request.professionalId())
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        Complaint complaint = new Complaint();
        complaint.setReporter(reporter);
        complaint.setProfessional(professional);
        if (request.appointmentId() != null) {
            complaint.setAppointment(appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> AppException.notFound("Appointment not found")));
        }
        complaint.setCategory(request.category().toUpperCase());
        complaint.setDescription(request.description());
        complaint.setStatus("OPEN");
        complaint.setPriority(request.priority() == null || request.priority().isBlank()
                ? "NORMAL"
                : request.priority().toUpperCase());
        complaint = complaintRepository.save(complaint);

        return toView(complaint);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintView> listByStatus(String status, Pageable pageable) {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable)
                .map(this::toView);
    }

    @Transactional
    public ComplaintView resolve(UUID adminId, UUID complaintId, ResolveComplaintRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> AppException.notFound("Admin user not found"));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> AppException.notFound("Complaint not found"));

        String fromStatus = complaint.getStatus();
        complaint.setStatus("RESOLVED");
        complaint.setResolutionNotes(request.resolutionNotes());
        complaint.setResolvedBy(admin);
        complaint.setResolvedAt(OffsetDateTime.now());

        if (request.suspendProfessional()) {
            Professional professional = complaint.getProfessional();
            professional.setVerificationStatus(VerificationStatus.SUSPENDED);
            professional.setAcceptingBookings(false);
            professionalRepository.save(professional);
        }

        complaint = complaintRepository.save(complaint);

        ComplaintEscalation escalation = new ComplaintEscalation();
        escalation.setComplaint(complaint);
        escalation.setEscalatedBy(admin);
        escalation.setFromStatus(fromStatus);
        escalation.setToStatus("RESOLVED");
        escalation.setReason(request.resolutionNotes());
        complaintEscalationRepository.save(escalation);

                notificationServiceV2Extension.sendComplaintResolvedNotice(complaint.getId());
        return toView(complaint);
    }

    @Transactional
    public ComplaintView dismiss(UUID adminId, UUID complaintId, String reason) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> AppException.notFound("Admin user not found"));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> AppException.notFound("Complaint not found"));

        String fromStatus = complaint.getStatus();
        complaint.setStatus("DISMISSED");
        complaint.setResolutionNotes(reason);
        complaint.setResolvedBy(admin);
        complaint.setResolvedAt(OffsetDateTime.now());
        complaint = complaintRepository.save(complaint);

        ComplaintEscalation escalation = new ComplaintEscalation();
        escalation.setComplaint(complaint);
        escalation.setEscalatedBy(admin);
        escalation.setFromStatus(fromStatus);
        escalation.setToStatus("DISMISSED");
        escalation.setReason(reason);
        complaintEscalationRepository.save(escalation);

        return toView(complaint);
    }

    private ComplaintView toView(Complaint complaint) {
        return new ComplaintView(
                complaint.getId(),
                complaint.getProfessional().getId(),
                complaint.getProfessional().getDisplayName(),
                complaint.getCategory(),
                complaint.getDescription(),
                complaint.getStatus(),
                complaint.getPriority(),
                complaint.getResolutionNotes(),
                complaint.getCreatedAt(),
                complaint.getResolvedAt());
    }

    public record FileComplaintRequest(
            UUID professionalId,
            UUID appointmentId,
            String category,
            String description,
            String priority
    ) {}

    public record ResolveComplaintRequest(String resolutionNotes, boolean suspendProfessional) {}

    public record ComplaintView(
            UUID id,
            UUID professionalId,
            String professionalName,
            String category,
            String description,
            String status,
            String priority,
            String resolutionNotes,
            OffsetDateTime createdAt,
            OffsetDateTime resolvedAt
    ) {}
}
