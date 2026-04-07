package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_documents")
@Getter
@Setter
public class VerificationDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType;

    @Column(name = "doc_url", nullable = false, columnDefinition = "TEXT")
    private String docUrl;

    @Column(name = "doc_hash", nullable = false, columnDefinition = "TEXT")
    private String docHash;

    @Column(name = "file_size_bytes")
    private Integer fileSizeBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt = OffsetDateTime.now();
}
