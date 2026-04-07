package com.appointunified.service;

import com.appointunified.entity.AdminAction;
import com.appointunified.entity.Professional;
import com.appointunified.entity.User;
import com.appointunified.entity.VerificationDocument;
import com.appointunified.enums.BadgeTier;
import com.appointunified.enums.VerificationStatus;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AdminActionRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.ProfessionalRepositoryHelper;
import com.appointunified.repository.UserRepository;
import com.appointunified.repository.VerificationDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VerificationService {

        private static final String CLOUDINARY_SAMPLE_PATH = "/image/upload/v1312461204/sample.jpg";

    private final ProfessionalRepository professionalRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final UserRepository userRepository;
    private final AdminActionRepository adminActionRepository;
    private final ProfessionalRepositoryHelper professionalRepositoryHelper;
    private final CloudinaryService cloudinaryService;
        private final NotificationService notificationService;
    private final NotificationServiceV2Extension notificationServiceV2Extension;

    public VerificationService(
            ProfessionalRepository professionalRepository,
            VerificationDocumentRepository verificationDocumentRepository,
            UserRepository userRepository,
            AdminActionRepository adminActionRepository,
            ProfessionalRepositoryHelper professionalRepositoryHelper,
            CloudinaryService cloudinaryService,
                        NotificationService notificationService,
            NotificationServiceV2Extension notificationServiceV2Extension) {
        this.professionalRepository = professionalRepository;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.userRepository = userRepository;
        this.adminActionRepository = adminActionRepository;
        this.professionalRepositoryHelper = professionalRepositoryHelper;
        this.cloudinaryService = cloudinaryService;
                this.notificationService = notificationService;
        this.notificationServiceV2Extension = notificationServiceV2Extension;
    }

    @Transactional(readOnly = true)
    public VerificationStatusView getStatus(UUID userId) {
        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Professional profile not found"));

        List<DocumentView> docs = verificationDocumentRepository.findByProfessionalIdOrderBySubmittedAtDesc(professional.getId())
                .stream()
                .map(d -> new DocumentView(
                        d.getId(),
                        d.getDocType(),
                        d.getDocUrl(),
                        d.getStatus(),
                        d.getReviewNotes(),
                        d.getSubmittedAt(),
                        d.getReviewedAt(),
                        d.getFileSizeBytes(),
                        d.getMimeType()))
                .toList();

        return new VerificationStatusView(
                professional.getVerificationStatus().name(),
                professional.getBadgeTier() != null ? professional.getBadgeTier().name() : BadgeTier.NONE.name(),
                professional.getVerificationExpiresAt(),
                docs);
    }

    @Transactional
    public DocumentView submitDocument(UUID userId, SubmitDocumentRequest request) {
        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Professional profile not found"));

                String normalizedUrl = request.docUrl() == null ? "" : request.docUrl().toLowerCase();
                if (request.docUrl() == null || request.docUrl().isBlank() || normalizedUrl.contains(CLOUDINARY_SAMPLE_PATH) || normalizedUrl.contains("sample.jpg?mock=")) {
                        throw AppException.badRequest("Please upload a real document file before submitting");
                }

        String rawForHash = request.docHash() != null && !request.docHash().isBlank()
                ? request.docHash()
                : request.docUrl();
        String hash = cloudinaryService.sha256Hex(rawForHash);

        if (verificationDocumentRepository.findByDocHash(hash).isPresent()) {
            throw AppException.conflict("This document is already submitted in the system");
        }

        VerificationDocument doc = new VerificationDocument();
        doc.setProfessional(professional);
        doc.setDocType(request.docType());
        doc.setDocUrl(request.docUrl());
        doc.setDocHash(hash);
        doc.setFileSizeBytes(request.fileSizeBytes());
        doc.setMimeType(request.mimeType());
        doc.setStatus("PENDING");
        doc.setSubmittedAt(OffsetDateTime.now());
        doc = verificationDocumentRepository.save(doc);

        if (professional.getVerificationStatus() == VerificationStatus.REJECTED) {
            professional.setVerificationStatus(VerificationStatus.PENDING);
            professionalRepository.save(professional);
        }

        return new DocumentView(
                doc.getId(),
                doc.getDocType(),
                doc.getDocUrl(),
                doc.getStatus(),
                doc.getReviewNotes(),
                doc.getSubmittedAt(),
                doc.getReviewedAt(),
                doc.getFileSizeBytes(),
                doc.getMimeType());
    }

    @Transactional(readOnly = true)
    public Page<PendingDocumentView> getPending(Pageable pageable) {
        return professionalRepositoryHelper.pendingVerificationDocuments(pageable)
                .map(d -> new PendingDocumentView(
                        d.getId(),
                        d.getProfessional().getId(),
                        d.getProfessional().getDisplayName(),
                        d.getProfessional().getSector().name(),
                        d.getDocType(),
                        d.getDocUrl(),
                        d.getFileSizeBytes(),
                        d.getSubmittedAt()));
    }

    @Transactional
    public DocumentView reviewDocument(UUID adminId, UUID docId, ReviewDocumentRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> AppException.notFound("Admin user not found"));

        VerificationDocument doc = verificationDocumentRepository.findById(docId)
                .orElseThrow(() -> AppException.notFound("Document not found"));

        String decision = request.decision().toUpperCase();
        if (!decision.equals("APPROVED") && !decision.equals("REJECTED")) {
            throw AppException.badRequest("Decision must be APPROVED or REJECTED");
        }

        doc.setStatus(decision);
        doc.setReviewedBy(admin);
        doc.setReviewNotes(request.notes());
        doc.setReviewedAt(OffsetDateTime.now());
        doc = verificationDocumentRepository.save(doc);

        AdminAction action = new AdminAction();
        action.setAdmin(admin);
        action.setActionType("REVIEW_DOCUMENT_" + decision);
        action.setTargetProfessional(doc.getProfessional());
        action.setTargetDocument(doc);
        action.setNotes(request.notes());
        adminActionRepository.save(action);

        Professional professional = doc.getProfessional();
        if ("REJECTED".equals(decision)) {
            professional.setVerificationStatus(VerificationStatus.REJECTED);
            professional.setAcceptingBookings(false);
            professionalRepository.save(professional);
        }

        return new DocumentView(
                doc.getId(),
                doc.getDocType(),
                doc.getDocUrl(),
                doc.getStatus(),
                doc.getReviewNotes(),
                doc.getSubmittedAt(),
                doc.getReviewedAt(),
                doc.getFileSizeBytes(),
                doc.getMimeType());
    }

    @Transactional
    public void approveProfessional(UUID adminId, UUID professionalId, String notes) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> AppException.notFound("Admin user not found"));

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        professional.setVerificationStatus(VerificationStatus.APPROVED);
        professional.setAcceptingBookings(true);
        if (professional.getBadgeTier() == null || professional.getBadgeTier() == BadgeTier.NONE) {
            professional.setBadgeTier(BadgeTier.BRONZE);
            professional.setBadgeAwardedAt(OffsetDateTime.now());
        }
        professional.setBadgeReviewedAt(OffsetDateTime.now());
        professional.setVerificationExpiresAt(OffsetDateTime.now().plusMonths(12));
        professional.setReverificationNotified(false);
        professionalRepository.save(professional);

        AdminAction action = new AdminAction();
        action.setAdmin(admin);
        action.setActionType("APPROVE_PROFESSIONAL");
        action.setTargetProfessional(professional);
        action.setNotes(notes);
        adminActionRepository.save(action);

        notificationServiceV2Extension.sendVerificationApproved(professional);
                notificationService.notifySuperAdminsProfessionalApproved(professional, admin);
    }

    @Transactional
    public void rejectProfessional(UUID adminId, UUID professionalId, String reason) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> AppException.notFound("Admin user not found"));

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        professional.setVerificationStatus(VerificationStatus.REJECTED);
        professional.setAcceptingBookings(false);
        professionalRepository.save(professional);

        AdminAction action = new AdminAction();
        action.setAdmin(admin);
        action.setActionType("REJECT_PROFESSIONAL");
        action.setTargetProfessional(professional);
        action.setNotes(reason);
        adminActionRepository.save(action);

        notificationServiceV2Extension.sendVerificationRejected(professional, reason);
    }

    public record SubmitDocumentRequest(
            String docType,
            String docUrl,
            String docHash,
            Integer fileSizeBytes,
            String mimeType
    ) {}

    public record ReviewDocumentRequest(String decision, String notes) {}

    public record DocumentView(
            UUID id,
            String docType,
            String docUrl,
            String status,
            String reviewNotes,
            OffsetDateTime submittedAt,
            OffsetDateTime reviewedAt,
            Integer fileSizeBytes,
            String mimeType
    ) {}

    public record PendingDocumentView(
            UUID docId,
            UUID professionalId,
            String professionalName,
            String sector,
            String docType,
            String docUrl,
            Integer fileSizeBytes,
            OffsetDateTime submittedAt
    ) {}

    public record VerificationStatusView(
            String verificationStatus,
            String badgeTier,
            OffsetDateTime verificationExpiresAt,
            List<DocumentView> documents
    ) {}
}
