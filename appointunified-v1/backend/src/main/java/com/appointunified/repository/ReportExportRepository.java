package com.appointunified.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.appointunified.entity.ReportExport;

@Repository
public interface ReportExportRepository extends JpaRepository<ReportExport, UUID> {
    
    /**
     * Find all export requests by a specific user, ordered by most recent first
     */
    List<ReportExport> findByRequestedByIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find all exports with READY status (available for download)
     */
    List<ReportExport> findByStatusOrderByCreatedAtDesc(String status);
    
    /**
     * Find all READY exports requested by a user
     */
    @Query("SELECT r FROM ReportExport r WHERE r.requestedBy.id = :userId AND r.status = 'READY' ORDER BY r.createdAt DESC")
    List<ReportExport> findReadyExportsByUser(@Param("userId") UUID userId);
    
    /**
     * Find all GENERATING exports (in progress)
     */
    List<ReportExport> findByStatusIn(List<String> statuses);
    
    /**
     * Find exports that have expired (past expiry date)
     */
    @Query("SELECT r FROM ReportExport r WHERE r.expiresAt < CURRENT_TIMESTAMP AND r.status != 'EXPIRED'")
    List<ReportExport> findExpiredExports();
}
