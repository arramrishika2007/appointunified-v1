package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "government_credentials")
@Getter
@Setter
public class GovernmentCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false, unique = true)
    private Professional professional;

    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;

    @Column(length = 255)
    private String department;

    @Column(name = "office_address", columnDefinition = "TEXT")
    private String officeAddress;

    @Column(name = "official_email", length = 255)
    private String officialEmail;

    @Column(name = "verified_employee", nullable = false)
    private boolean verifiedEmployee = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
