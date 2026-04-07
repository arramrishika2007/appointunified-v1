package com.appointunified.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.appointunified.entity.AnalyticsSnapshot;

@Repository
public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {
    
    /**
     * Find all snapshots for a specific date and sector
     */
    Optional<AnalyticsSnapshot> findBySnapshotDateAndSector(LocalDate snapshotDate, String sector);
    
    /**
     * Find platform-wide snapshot (sector = null) for a specific date
     */
    @Query("SELECT a FROM AnalyticsSnapshot a WHERE a.snapshotDate = :date AND a.sector IS NULL")
    Optional<AnalyticsSnapshot> findPlatformWideSnapshot(@Param("date") LocalDate date);
    
    /**
     * Find snapshots within a date range for a specific sector
     */
    List<AnalyticsSnapshot> findBySnapshotDateBetweenAndSectorOrderBySnapshotDateDesc(
        LocalDate startDate, LocalDate endDate, String sector);
    
    /**
     * Find platform-wide snapshots within a date range
     */
    @Query("SELECT a FROM AnalyticsSnapshot a WHERE a.snapshotDate BETWEEN :startDate AND :endDate AND a.sector IS NULL ORDER BY a.snapshotDate DESC")
    List<AnalyticsSnapshot> findPlatformWideSnapshotsBetweenDates(
        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Find all sector snapshots for a date range
     */
    List<AnalyticsSnapshot> findBySnapshotDateBetweenOrderBySnapshotDateDesc(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get latest snapshot for a sector
     */
    @Query("SELECT a FROM AnalyticsSnapshot a WHERE a.sector = :sector ORDER BY a.snapshotDate DESC LIMIT 1")
    Optional<AnalyticsSnapshot> findLatestBySector(@Param("sector") String sector);
}
