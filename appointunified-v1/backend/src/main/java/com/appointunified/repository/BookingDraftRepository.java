package com.appointunified.repository;

import com.appointunified.entity.BookingDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingDraftRepository extends JpaRepository<BookingDraft, UUID> {
    @EntityGraph(attributePaths = {"professional", "service"})
    List<BookingDraft> findByUserIdAndExpiresAtAfterOrderByUpdatedAtDesc(UUID userId, OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM BookingDraft d WHERE d.expiresAt < :now")
    int deleteExpiredDrafts(OffsetDateTime now);
}
