package com.appointunified.repository;
import com.appointunified.entity.ClientQueuePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientQueuePreferenceRepository extends JpaRepository<ClientQueuePreference, UUID> {
    Optional<ClientQueuePreference> findByUserId(UUID userId);
}
