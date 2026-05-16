package com.appointunified.service;

import com.appointunified.dto.request.ProfessionalRequest;
import com.appointunified.dto.response.AppointmentResponse;
import com.appointunified.dto.response.ProfessionalResponse;
import com.appointunified.dto.response.ServiceResponse;
import com.appointunified.entity.Availability;
import com.appointunified.entity.GeoZone;
import com.appointunified.entity.Professional;
import com.appointunified.entity.User;
import com.appointunified.enums.Sector;
import com.appointunified.enums.VerificationStatus;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.AvailabilityRepository;
import com.appointunified.repository.GeoZoneRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.ServiceRepository;
import com.appointunified.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.springframework.stereotype.Service
public class ProfessionalService {

        private static final Logger log = LoggerFactory.getLogger(ProfessionalService.class);

    private final ProfessionalRepository professionalRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final GeoZoneRepository geoZoneRepository;

        public ProfessionalService(ProfessionalRepository professionalRepository,
                                                                UserRepository userRepository,
                                                                ServiceRepository serviceRepository,
                                                                AvailabilityRepository availabilityRepository,
                                                                AppointmentRepository appointmentRepository,
                                                                GeoZoneRepository geoZoneRepository) {
                this.professionalRepository = professionalRepository;
                this.userRepository = userRepository;
                this.serviceRepository = serviceRepository;
                this.availabilityRepository = availabilityRepository;
                this.appointmentRepository = appointmentRepository;
                                        this.geoZoneRepository = geoZoneRepository;
        }

    public ProfessionalResponse.Detail getMyProfile(UUID userId) {
        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Professional profile not found. Please complete onboarding."));
        return toDetailResponse(professional);
    }

    @Transactional
        @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public ProfessionalResponse.Detail register(UUID userId, ProfessionalRequest.Register request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (professionalRepository.findByUserId(userId).isPresent()) {
            throw AppException.conflict("Professional profile already exists for this account");
        }

        Sector sector;
        try {
            sector = Sector.valueOf(request.getSector().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw AppException.badRequest("Invalid sector. Allowed values: HEALTHCARE, GOVERNMENT, SERVICES");
        }

        Professional professional = new Professional();
        professional.setUser(user);
        professional.setDisplayName(request.getDisplayName());
        professional.setSector(sector);
        professional.setSpecialty(normalizeOptionalText(request.getSpecialty()));
        professional.setLicenseNumber(normalizeOptionalText(request.getLicenseNumber()));
        professional.setBio(normalizeOptionalText(request.getBio()));
        professional.setQualification(normalizeOptionalText(request.getQualification()));
        professional.setYearsExperience(request.getYearsExperience());
        professional.setConsultationFee(request.getConsultationFee());
        professional.setCity(normalizeOptionalText(request.getCity()));
        professional.setAddress(normalizeOptionalText(request.getAddress()));
        professional.setUpiId(normalizeOptionalText(request.getUpiId()));
        
        // Use provided lat/lon, or fallback to Geocoding
        if (request.getLatitude() != null && request.getLongitude() != null) {
            professional.setLatitude(request.getLatitude());
            professional.setLongitude(request.getLongitude());
        } else if (professional.getAddress() != null) {
            double[] coords = geocodeAddress(professional.getAddress() + ", " + professional.getCity());
            if (coords != null) {
                professional.setLatitude(java.math.BigDecimal.valueOf(coords[0]));
                professional.setLongitude(java.math.BigDecimal.valueOf(coords[1]));
            }
        }
        
        professional.setServiceAreaRadiusKm(request.getServiceAreaRadiusKm());
        professional.setVerificationStatus(VerificationStatus.PENDING); // Require admin approval
        professional.setAcceptingBookings(false); // Do not accept bookings until approved 

        try {
            professional = professionalRepository.save(professional);

            // Save weekly schedule if provided
            if (request.getWeeklySchedule() != null) {
                saveAvailability(professional, request.getWeeklySchedule());
            }

            seedDefaultServicesIfMissing(professional, request.getConsultationFee());
        } catch (DataIntegrityViolationException ex) {
            String detail = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();
            if (detail != null && detail.toLowerCase().contains("license_number")) {
                throw AppException.conflict("License number is already in use");
            }
            throw AppException.conflict("Unable to register profile due to conflicting data");
        }

        log.info("Professional registered: {} (sector: {})", professional.getId(), sector);
        return toDetailResponse(professional);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillDefaultServicesOnStartup() {
        List<Professional> professionals = professionalRepository.findAll();
        int seeded = 0;
        for (Professional professional : professionals) {
            if (serviceRepository.findByProfessionalIdAndActiveTrue(professional.getId()).isEmpty()) {
                seedDefaultServicesIfMissing(professional, professional.getConsultationFee());
                seeded++;
            }
        }
        if (seeded > 0) {
            log.info("Seeded default services for {} professionals", seeded);
        }
    }

    @Transactional
        @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public ProfessionalResponse.Detail updateProfile(UUID userId, ProfessionalRequest.Update request) {
        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Professional profile not found"));

        if (request.getDisplayName() != null) professional.setDisplayName(request.getDisplayName());
        if (request.getBio() != null) professional.setBio(request.getBio());
        if (request.getQualification() != null) professional.setQualification(request.getQualification());
        if (request.getYearsExperience() != null) professional.setYearsExperience(request.getYearsExperience());
        if (request.getConsultationFee() != null) professional.setConsultationFee(request.getConsultationFee());
        if (request.getCity() != null) professional.setCity(request.getCity());
        if (request.getAddress() != null) professional.setAddress(request.getAddress());
        if (request.getLatitude() != null) professional.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) professional.setLongitude(request.getLongitude());
        if (request.getAvatarUrl() != null) professional.setAvatarUrl(request.getAvatarUrl());
        if (request.getCoverUrl() != null) professional.setCoverUrl(request.getCoverUrl());
        if (request.getAcceptingBookings() != null) professional.setAcceptingBookings(request.getAcceptingBookings());
        if (request.getUpiId() != null) professional.setUpiId(request.getUpiId());
        if (request.getServiceAreaRadiusKm() != null) professional.setServiceAreaRadiusKm(request.getServiceAreaRadiusKm());

        // Geocoding on update
        if (request.getAddress() != null && (request.getLatitude() == null || request.getLongitude() == null)) {
            double[] coords = geocodeAddress(professional.getAddress() + ", " + professional.getCity());
            if (coords != null) {
                professional.setLatitude(java.math.BigDecimal.valueOf(coords[0]));
                professional.setLongitude(java.math.BigDecimal.valueOf(coords[1]));
            }
        }

        return toDetailResponse(professionalRepository.save(professional));
    }

    public List<ProfessionalResponse.Summary> searchNearby(double lat, double lng, double radiusKm, String sector, int limit) {
        Sector sectorEnum = null;
        if (sector != null && !sector.isBlank()) {
            try {
                sectorEnum = Sector.valueOf(sector.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw AppException.badRequest("Invalid sector filter. Allowed values: HEALTHCARE, GOVERNMENT, SERVICES");
            }
        }

        List<Professional> all = professionalRepository.findByVerificationStatusAndAcceptingBookingsTrue(
                VerificationStatus.APPROVED,
                org.springframework.data.domain.Pageable.ofSize(Math.max(100, limit * 4))
        ).getContent();

        double requestedRadius = Math.max(1.0, radiusKm);
        final Sector sectorFilter = sectorEnum;

        return all.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .filter(p -> sectorFilter == null || p.getSector() == sectorFilter)
                .map(p -> {
                    double distance = haversineKm(lat, lng, p.getLatitude().doubleValue(), p.getLongitude().doubleValue());
                    BigDecimal providerRadius = p.getServiceAreaRadiusKm() != null ? p.getServiceAreaRadiusKm() : BigDecimal.valueOf(requestedRadius);
                    boolean insideRequested = distance <= requestedRadius;
                    boolean insideProviderArea = distance <= providerRadius.doubleValue();
                    if (!insideRequested || !insideProviderArea) {
                        return null;
                    }
                    ProfessionalResponse.Summary summary = toSummaryResponse(p);
                    summary.setDistanceKm(BigDecimal.valueOf(distance).setScale(2, java.math.RoundingMode.HALF_UP));
                    return summary;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProfessionalResponse.Summary::getDistanceKm))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfessionalResponse.Summary updateServiceArea(UUID userId,
                                                          UUID professionalId,
                                                          BigDecimal radiusKm,
                                                          BigDecimal centerLat,
                                                          BigDecimal centerLng) {
        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Professional profile not found"));

        if (!professional.getId().equals(professionalId)) {
            throw AppException.forbidden("You can only update your own service area");
        }

        if (radiusKm == null || radiusKm.doubleValue() <= 0.0) {
            throw AppException.badRequest("radiusKm must be greater than 0");
        }

        BigDecimal lat = centerLat != null ? centerLat : professional.getLatitude();
        BigDecimal lng = centerLng != null ? centerLng : professional.getLongitude();
        if (lat == null || lng == null) {
            throw AppException.badRequest("centerLat and centerLng are required when profile has no coordinates");
        }

        professional.setServiceAreaRadiusKm(radiusKm);
        professional.setLatitude(lat);
        professional.setLongitude(lng);
        Professional savedProfessional = professionalRepository.save(professional);

        GeoZone zone = geoZoneRepository.findByProfessionalIdAndActiveTrue(savedProfessional.getId())
                .orElseGet(() -> {
                    GeoZone created = new GeoZone();
                    created.setProfessional(savedProfessional);
                    created.setActive(true);
                    return created;
                });
        zone.setCenterLat(lat);
        zone.setCenterLng(lng);
        zone.setRadiusKm(radiusKm);
        geoZoneRepository.save(zone);

        return toSummaryResponse(savedProfessional);
    }

    @Transactional
    public ProfessionalResponse.Summary updateOverbooking(UUID userId, UUID professionalId, boolean allowOverbooking) {
        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Professional profile not found"));

        if (!professional.getId().equals(professionalId)) {
            throw AppException.forbidden("You can only update your own overbooking setting");
        }

        professional.setAllowOverbooking(allowOverbooking);
        professional = professionalRepository.save(professional);
        return toSummaryResponse(professional);
    }

    // NEW V1 FEATURE 2: Update mood status
    @Transactional
        @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public ProfessionalResponse.Summary updateMood(UUID userId, ProfessionalRequest.MoodUpdate request) {
        Professional professional = professionalRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Professional profile not found"));

        professional.setAvailabilityMood(request.getMood());
        professional.setMoodNote(request.getNote());
        professional.setMoodUpdatedAt(OffsetDateTime.now());

        professional = professionalRepository.save(professional);
        log.info("Professional {} mood updated to {}", professional.getId(), request.getMood());
        return toSummaryResponse(professional);
    }

    public Page<ProfessionalResponse.Summary> searchVerified(String sector, String city,
                                                               String query, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            List<Professional> results;
            try {
            results = professionalRepository.fuzzySearch(query);
            } catch (DataAccessException ex) {
            // Fallback when pg_trgm is unavailable or native fuzzy query fails.
            results = professionalRepository
                .findTop20ByVerificationStatusAndAcceptingBookingsTrueAndDisplayNameContainingIgnoreCaseOrVerificationStatusAndAcceptingBookingsTrueAndSpecialtyContainingIgnoreCase(
                    VerificationStatus.APPROVED,
                    query,
                    VerificationStatus.APPROVED,
                    query
                );
            }
            // Convert list to page manually for fuzzy search
            return new org.springframework.data.domain.PageImpl<>(
                    results.stream().map(this::toSummaryResponse).collect(Collectors.toList()),
                    pageable, results.size());
        }

        Sector sectorEnum;
        try {
            sectorEnum = (sector != null && !sector.isBlank())
                    ? Sector.valueOf(sector.toUpperCase())
                    : null;
        } catch (IllegalArgumentException ex) {
            throw AppException.badRequest("Invalid sector filter. Allowed values: HEALTHCARE, GOVERNMENT, SERVICES");
        }

        String cityFilter = city != null && !city.isBlank() ? city.trim() : null;
        Page<Professional> page;

        if (sectorEnum != null && cityFilter != null) {
            page = professionalRepository.findByVerificationStatusAndAcceptingBookingsTrueAndSectorAndCityContainingIgnoreCase(
                VerificationStatus.APPROVED,
                sectorEnum,
                cityFilter,
                pageable
            );
        } else if (sectorEnum != null) {
            page = professionalRepository.findByVerificationStatusAndAcceptingBookingsTrueAndSector(
                VerificationStatus.APPROVED,
                sectorEnum,
                pageable
            );
        } else if (cityFilter != null) {
            page = professionalRepository.findByVerificationStatusAndAcceptingBookingsTrueAndCityContainingIgnoreCase(
                VerificationStatus.APPROVED,
                cityFilter,
                pageable
            );
        } else {
            page = professionalRepository.findByVerificationStatusAndAcceptingBookingsTrue(
                VerificationStatus.APPROVED,
                pageable
            );
        }

        return page.map(this::toSummaryResponse);
    }

    public ProfessionalResponse.Detail getById(UUID id) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Professional not found"));
        return toDetailResponse(professional);
    }

    // ─── Slot calculation ───────────────────────────────────────────────────

    public List<AppointmentResponse.AvailableSlot> getAvailableSlots(UUID professionalId, LocalDate date,
                                                                       UUID serviceId) {
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        if (!professional.isAcceptingBookings()) {
            return Collections.emptyList();
        }

        // Get availability for this weekday (0=Mon ... 6=Sun)
        int weekday = date.getDayOfWeek().getValue() - 1; // Java: MON=1, we want 0
        List<Availability> availabilities = availabilityRepository
                .findByProfessionalIdAndActiveTrue(professionalId);

        Optional<Availability> daySchedule = availabilities.stream()
                .filter(a -> a.getWeekday() == weekday)
                .findFirst();

        if (daySchedule.isEmpty()) {
            return Collections.emptyList();
        }

        Availability schedule = daySchedule.get();
        ZoneOffset zone = ZoneOffset.of("+05:30");

        OffsetDateTime dayStart = OffsetDateTime.of(date, schedule.getStartTime(), zone);
OffsetDateTime dayEnd = OffsetDateTime.of(date, schedule.getEndTime(), zone);

        // Get booked slots for this day
        List<com.appointunified.entity.Appointment> bookedSlots =
                appointmentRepository.findBookedSlots(professionalId, dayStart, dayEnd);

        // Generate all possible slots
        List<AppointmentResponse.AvailableSlot> slots = new ArrayList<>();
        OffsetDateTime cursor = dayStart;
        int slotDuration = schedule.getSlotDurationMins();
        int buffer = schedule.getBufferMinutes();

        while (cursor.plusMinutes(slotDuration).compareTo(dayEnd) <= 0) {
            final OffsetDateTime slotStart = cursor;
            final OffsetDateTime slotEnd = cursor.plusMinutes(slotDuration);

            boolean isBooked = bookedSlots.stream().anyMatch(appt ->
                    appt.getStartTime().isBefore(slotEnd) && appt.getEndTime().isAfter(slotStart));

                    boolean isPast = slotStart.isBefore(OffsetDateTime.now(java.time.ZoneOffset.of("+05:30")).plusMinutes(30));

            AppointmentResponse.AvailableSlot availableSlot = new AppointmentResponse.AvailableSlot();
            availableSlot.setStartTime(slotStart);
            availableSlot.setEndTime(slotEnd);
            availableSlot.setAvailable(!isBooked && !isPast);
            availableSlot.setLabel(slotStart.toLocalTime().toString());
            slots.add(availableSlot);

            cursor = slotEnd.plusMinutes(buffer);
        }

        return slots;
    }

    // ─── Mappers ────────────────────────────────────────────────────────────

    private ProfessionalResponse.Summary toSummaryResponse(Professional p) {
        ProfessionalResponse.Summary summary = new ProfessionalResponse.Summary();
        summary.setId(p.getId());
        summary.setDisplayName(p.getDisplayName());
        summary.setSector(p.getSector().name());
        summary.setSpecialty(p.getSpecialty());
        summary.setVerificationStatus(p.getVerificationStatus().name());
        summary.setRatingAvg(p.getRatingAvg());
        summary.setTotalReviews(p.getTotalReviews());
        summary.setAvatarUrl(p.getAvatarUrl());
        summary.setConsultationFee(p.getConsultationFee());
        summary.setCity(p.getCity());
        summary.setLatitude(p.getLatitude());
        summary.setLongitude(p.getLongitude());
        summary.setServiceAreaRadiusKm(p.getServiceAreaRadiusKm());
        summary.setAcceptingBookings(p.isAcceptingBookings());
        summary.setAvailabilityMood(p.getAvailabilityMood() != null ? p.getAvailabilityMood().name() : null);
        summary.setMoodNote(p.getMoodNote());
        summary.setBadgeTier(p.getBadgeTier() != null ? p.getBadgeTier().name() : null);
        summary.setAllowOverbooking(p.isAllowOverbooking());
        summary.setUpiId(p.getUpiId());
        return summary;
    }

    private ProfessionalResponse.Detail toDetailResponse(Professional p) {
        List<Availability> avails = availabilityRepository.findByProfessionalIdAndActiveTrue(p.getId());
        List<com.appointunified.entity.Service> services = serviceRepository.findByProfessionalIdAndActiveTrue(p.getId());
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        List<ProfessionalResponse.AvailabilityDay> schedule = avails.stream()
                .map(a -> {
                    ProfessionalResponse.AvailabilityDay day = new ProfessionalResponse.AvailabilityDay();
                    day.setWeekday(a.getWeekday());
                    day.setDayName(days[a.getWeekday()]);
                    day.setStartTime(a.getStartTime());
                    day.setEndTime(a.getEndTime());
                    day.setBufferMinutes(a.getBufferMinutes());
                    day.setSlotDurationMins(a.getSlotDurationMins());
                    return day;
                })
                .sorted(Comparator.comparing(ProfessionalResponse.AvailabilityDay::getWeekday))
                .collect(Collectors.toList());

        List<ServiceResponse.Summary> serviceSummaries = services.stream()
                .map(s -> {
                    ServiceResponse.Summary summary = new ServiceResponse.Summary();
                    summary.setId(s.getId());
                    summary.setName(s.getName());
                    summary.setDescription(s.getDescription());
                    summary.setDurationMinutes(s.getDurationMinutes());
                    summary.setPrice(s.getPrice());
                    summary.setIsActive(s.isActive());
                    summary.setRequiresDocuments(s.isRequiresDocuments());
                    summary.setIsVirtual(s.isVirtual());
                    return summary;
                })
                .collect(Collectors.toList());

        ProfessionalResponse.Detail detail = new ProfessionalResponse.Detail();
        detail.setId(p.getId());
        detail.setDisplayName(p.getDisplayName());
        detail.setSector(p.getSector().name());
        detail.setSpecialty(p.getSpecialty());
        detail.setVerificationStatus(p.getVerificationStatus().name());
        detail.setRatingAvg(p.getRatingAvg());
        detail.setTotalReviews(p.getTotalReviews());
        detail.setTotalCompleted(p.getTotalCompleted());
        detail.setAvatarUrl(p.getAvatarUrl());
        detail.setCoverUrl(p.getCoverUrl());
        detail.setConsultationFee(p.getConsultationFee());
        detail.setCity(p.getCity());
        detail.setAddress(p.getAddress());
        detail.setLatitude(p.getLatitude());
        detail.setLongitude(p.getLongitude());
        detail.setServiceAreaRadiusKm(p.getServiceAreaRadiusKm());
        detail.setAcceptingBookings(p.isAcceptingBookings());
        detail.setBio(p.getBio());
        detail.setQualification(p.getQualification());
        detail.setYearsExperience(p.getYearsExperience());
        detail.setLicenseNumber(p.getLicenseNumber());
        detail.setAvailabilityMood(p.getAvailabilityMood() != null ? p.getAvailabilityMood().name() : null);
        detail.setMoodNote(p.getMoodNote());
        detail.setBadgeTier(p.getBadgeTier() != null ? p.getBadgeTier().name() : null);
        detail.setAllowOverbooking(p.isAllowOverbooking());
        detail.setUpiId(p.getUpiId());
        detail.setVerificationExpiresAt(p.getVerificationExpiresAt());
        detail.setServices(serviceSummaries);
        detail.setWeeklySchedule(schedule);
        detail.setJoinedAt(p.getCreatedAt());
        return detail;
    }

    private void saveAvailability(Professional professional, List<ProfessionalRequest.AvailabilitySlot> slots) {
        // Clear existing
        availabilityRepository.findByProfessionalIdAndActiveTrue(professional.getId())
                .forEach(a -> { a.setActive(false); availabilityRepository.save(a); });

        slots.forEach(slot -> {
                        Availability a = new Availability();
                        a.setProfessional(professional);
                        a.setWeekday(slot.getWeekday());
                        a.setStartTime(slot.getStartTime());
                        a.setEndTime(slot.getEndTime());
                        a.setBufferMinutes(slot.getBufferMinutes());
                        a.setSlotDurationMins(slot.getSlotDurationMins());
                        a.setActive(true);
            availabilityRepository.save(a);
        });
    }

    private void seedDefaultServicesIfMissing(Professional professional, java.math.BigDecimal consultationFee) {
        if (!serviceRepository.findByProfessionalIdAndActiveTrue(professional.getId()).isEmpty()) {
            return;
        }

        java.util.List<ServiceTemplate> templates = defaultServiceTemplates(professional.getSector());
        for (ServiceTemplate template : templates) {
            com.appointunified.entity.Service service = new com.appointunified.entity.Service();
            service.setProfessional(professional);
            service.setSector(professional.getSector());
            service.setName(template.name);
            service.setDescription(template.description);
            service.setDurationMinutes(template.durationMinutes);
            service.setPrice(template.price != null ? template.price : consultationFee);
            service.setRequiresDocuments(template.requiresDocuments);
            service.setVirtual(template.virtual);
            service.setMaxConcurrent((short) 1);
            service.setActive(true);
            service.setCreatedAt(OffsetDateTime.now());
            service.setUpdatedAt(OffsetDateTime.now());
            serviceRepository.save(service);
        }
    }

    private List<ServiceTemplate> defaultServiceTemplates(Sector sector) {
        return switch (sector) {
            case HEALTHCARE -> List.of(
                    new ServiceTemplate("Consultation", "General in-person consultation", 30, null, false, false),
                    new ServiceTemplate("Follow-up Visit", "Short follow-up or review session", 15, null, false, false),
                    new ServiceTemplate("Health Checkup", "Basic preventive health checkup", 45, null, false, false)
            );
            case GOVERNMENT -> List.of(
                    new ServiceTemplate("Appointment Assistance", "Help with appointments and submissions", 20, null, true, false),
                    new ServiceTemplate("Document Verification", "Review and verify submitted documents", 30, null, true, false),
                    new ServiceTemplate("Grievance Consultation", "Public service or complaint consultation", 25, null, false, false)
            );
            case SERVICES -> List.of(
                    new ServiceTemplate("Site Visit", "On-site service visit and inspection", 45, null, false, false),
                    new ServiceTemplate("Repair / Maintenance", "Standard repair or maintenance service", 60, null, false, false),
                    new ServiceTemplate("Emergency Support", "Priority emergency assistance", 30, null, false, true)
            );
        };
    }

    private record ServiceTemplate(
            String name,
            String description,
            Integer durationMinutes,
            java.math.BigDecimal price,
            boolean requiresDocuments,
            boolean virtual
    ) {}

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double[] geocodeAddress(String address) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" +
                    java.net.URLEncoder.encode(address, java.nio.charset.StandardCharsets.UTF_8) +
                    "&format=jsonv2&limit=1";
            
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "AppointUnified-BookingApp/1.0");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().equals("[]")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response.getBody());
                com.fasterxml.jackson.databind.JsonNode firstResult = root.get(0);
                if (firstResult != null) {
                    double lat = Double.parseDouble(firstResult.get("lat").asText());
                    double lon = Double.parseDouble(firstResult.get("lon").asText());
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for address: " + address, e);
        }
        return null;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
