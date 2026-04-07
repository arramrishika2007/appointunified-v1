package com.appointunified.repository;

import com.appointunified.entity.HealthcareCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HealthcareCredentialsRepository extends JpaRepository<HealthcareCredentials, UUID> {
    Optional<HealthcareCredentials> findByProfessionalId(UUID professionalId);
}
