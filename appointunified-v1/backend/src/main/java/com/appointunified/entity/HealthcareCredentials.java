package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "healthcare_credentials")
@Getter
@Setter
public class HealthcareCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false, unique = true)
    private Professional professional;

    @Column(name = "nmc_registration", length = 50)
    private String nmcRegistration;

    @Column(length = 50)
    private String degree;

    @Column(name = "hospital_affiliation", columnDefinition = "TEXT")
    private String hospitalAffiliation;

    @Column(name = "specialization_code", length = 20)
    private String specializationCode;

    @Column(name = "verified_nmc", nullable = false)
    private boolean verifiedNmc = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
