package com.appointunified.repository;

import com.appointunified.entity.GovernmentCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GovernmentCredentialsRepository extends JpaRepository<GovernmentCredentials, UUID> {
    Optional<GovernmentCredentials> findByProfessionalId(UUID professionalId);
}
