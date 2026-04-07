package com.appointunified.repository;

import com.appointunified.entity.GeoZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeoZoneRepository extends JpaRepository<GeoZone, UUID> {
    Optional<GeoZone> findByProfessionalIdAndActiveTrue(UUID professionalId);
}
