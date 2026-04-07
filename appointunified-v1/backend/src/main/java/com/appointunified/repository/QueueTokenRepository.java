package com.appointunified.repository;
import com.appointunified.entity.QueueToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueTokenRepository extends JpaRepository<QueueToken, UUID> {
    Optional<QueueToken> findByAppointmentId(UUID appointmentId);
    List<QueueToken> findByProfessionalIdAndStatusOrderByPosition(UUID professionalId, String status);
    List<QueueToken> findByProfessionalIdAndStatusInOrderByPosition(UUID professionalId, List<String> statuses);

    @Query("SELECT COALESCE(MAX(t.tokenNumber), 0) FROM QueueToken t WHERE t.professional.id = :profId")
    Integer findMaxTokenNumber(@Param("profId") UUID professionalId);

    @Query("SELECT COUNT(t) FROM QueueToken t WHERE t.professional.id = :profId AND t.status = 'WAITING'")
    long countWaiting(@Param("profId") UUID professionalId);
}
