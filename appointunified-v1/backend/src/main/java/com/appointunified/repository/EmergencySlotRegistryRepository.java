package com.appointunified.repository;
import com.appointunified.entity.EmergencySlotRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmergencySlotRegistryRepository extends JpaRepository<EmergencySlotRegistry, UUID> {
    List<EmergencySlotRegistry> findByProfessionalIdOrderByInsertedAtDesc(UUID profId);
}
