package com.appointunified.repository;

import com.appointunified.entity.Professional;
import com.appointunified.enums.Sector;
import com.appointunified.enums.VerificationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {
    Optional<Professional> findByUserId(UUID userId);

       @EntityGraph(attributePaths = {"user"})
       Optional<Professional> findWithUserById(UUID id);

    Page<Professional> findBySectorAndVerificationStatus(Sector sector, VerificationStatus status, Pageable pageable);

       @EntityGraph(attributePaths = {"user"})
    Page<Professional> findByVerificationStatus(VerificationStatus status, Pageable pageable);

    Page<Professional> findByVerificationStatusAndAcceptingBookingsTrue(
           VerificationStatus verificationStatus,
           Pageable pageable);

    Page<Professional> findByVerificationStatusAndAcceptingBookingsTrueAndSector(
           VerificationStatus verificationStatus,
           Sector sector,
           Pageable pageable);

    Page<Professional> findByVerificationStatusAndAcceptingBookingsTrueAndCityContainingIgnoreCase(
           VerificationStatus verificationStatus,
           String city,
           Pageable pageable);

    Page<Professional> findByVerificationStatusAndAcceptingBookingsTrueAndSectorAndCityContainingIgnoreCase(
           VerificationStatus verificationStatus,
           Sector sector,
           String city,
           Pageable pageable);

    @Query(value = "SELECT * FROM professionals WHERE verification_status = 'APPROVED' " +
                   "AND is_accepting_bookings = true " +
                   "AND (display_name % :query OR specialty % :query) " +
                   "ORDER BY similarity(display_name, :query) DESC LIMIT 20",
           nativeQuery = true)
    List<Professional> fuzzySearch(@Param("query") String query);

    List<Professional> findTop20ByVerificationStatusAndAcceptingBookingsTrueAndDisplayNameContainingIgnoreCaseOrVerificationStatusAndAcceptingBookingsTrueAndSpecialtyContainingIgnoreCase(
           VerificationStatus statusForName,
           String displayName,
           VerificationStatus statusForSpecialty,
           String specialty);

    List<Professional> findByVerificationStatusAndVerificationExpiresAtBeforeAndReverificationNotifiedFalse(
           VerificationStatus verificationStatus,
           OffsetDateTime verificationExpiresAt);
}
