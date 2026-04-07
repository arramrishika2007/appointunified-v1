package com.appointunified.repository;

import com.appointunified.entity.ServicesCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServicesCredentialsRepository extends JpaRepository<ServicesCredentials, UUID> {
    Optional<ServicesCredentials> findByProfessionalId(UUID professionalId);
}
