package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "services_credentials")
@Getter
@Setter
public class ServicesCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false, unique = true)
    private Professional professional;

    @Column(name = "trade_skill", length = 100)
    private String tradeSkill;

    @Column(name = "aadhaar_verified", nullable = false)
    private boolean aadhaarVerified = false;

    @Column(name = "certifications", columnDefinition = "TEXT[]")
    private String[] certifications = new String[0];

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
