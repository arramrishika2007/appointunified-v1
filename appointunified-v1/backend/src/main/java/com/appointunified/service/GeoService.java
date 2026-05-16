package com.appointunified.service;

import com.appointunified.dto.response.GeoResponse;
import com.appointunified.entity.Appointment;
import com.appointunified.entity.Professional;
import com.appointunified.entity.RouteCache;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.RouteCacheRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.appointunified.enums.AppointmentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoService {

    private final RouteCacheRepository routeCacheRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;

    @Value("${OPENROUTESERVICE_API_KEY:}")
    private String orsApiKey;

    @Transactional(readOnly = true)
    public GeoResponse.TravelTime getTravelTime(double fromLat, double fromLng, double toLat, double toLng) {
        String fromHash = hashPoint(fromLat, fromLng);
        String toHash = hashPoint(toLat, toLng);

        Optional<RouteCache> cached = routeCacheRepository.findByFromHashAndToHash(fromHash, toHash);
        if (cached.isPresent()) {
            GeoResponse.TravelTime response = new GeoResponse.TravelTime();
            response.setDistanceKm(cached.get().getDistanceKm());
            response.setDurationMinutes(cached.get().getDurationMinutes());
            response.setSummary("Cached route estimate");
            return response;
        }

        GeoResponse.TravelTime travel = fetchTravelTimeFromOrs(fromLat, fromLng, toLat, toLng)
                .orElseGet(() -> estimateFallback(fromLat, fromLng, toLat, toLng));

        RouteCache cache = new RouteCache();
        cache.setFromHash(fromHash);
        cache.setToHash(toHash);
        cache.setDistanceKm(travel.getDistanceKm());
        cache.setDurationMinutes(travel.getDurationMinutes());
        cache.setCachedAt(OffsetDateTime.now());
        routeCacheRepository.save(cache);

        return travel;
    }

    @Transactional(readOnly = true)
    public GeoResponse.RouteOptimize optimizeProfessionalRoute(UUID professionalUserId, LocalDate date) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        OffsetDateTime from = OffsetDateTime.of(date, java.time.LocalTime.MIN, ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.of(date, java.time.LocalTime.MAX, ZoneOffset.UTC);

        List<Appointment> all = appointmentRepository.findBookedSlots(professional.getId(), from, to,
    List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW, AppointmentStatus.EXPIRED));
        List<Appointment> offline = all.stream()
                .filter(a -> !a.isVirtual() && a.getClientLat() != null && a.getClientLon() != null)
                .sorted(Comparator.comparing(Appointment::getStartTime))
                .toList();

        GeoResponse.RouteOptimize response = new GeoResponse.RouteOptimize();
        response.setProfessionalId(professional.getId());
        response.setDateStart(from);
        response.setOfflineAppointments(offline.size());

        List<GeoResponse.RouteLeg> legs = new ArrayList<>();
        BigDecimal totalDistance = BigDecimal.ZERO;
        int totalMinutes = 0;

        for (int i = 0; i < offline.size() - 1; i++) {
            Appointment current = offline.get(i);
            Appointment next = offline.get(i + 1);
            GeoResponse.TravelTime travel = getTravelTime(
                    current.getClientLat(),
                    current.getClientLon(),
                    next.getClientLat(),
                    next.getClientLon()
            );

            GeoResponse.RouteLeg leg = new GeoResponse.RouteLeg();
            leg.setFromAppointmentId(current.getId());
            leg.setToAppointmentId(next.getId());
            leg.setFromClient(current.getClient().getFullName());
            leg.setToClient(next.getClient().getFullName());
            leg.setFromStartTime(current.getStartTime());
            leg.setToStartTime(next.getStartTime());
            leg.setDistanceKm(travel.getDistanceKm());
            leg.setDurationMinutes(travel.getDurationMinutes());
            legs.add(leg);

            totalDistance = totalDistance.add(travel.getDistanceKm());
            totalMinutes += travel.getDurationMinutes();
        }

        response.setLegs(legs);
        response.setTotalDistanceKm(totalDistance.setScale(2, RoundingMode.HALF_UP));
        response.setTotalTravelMinutes(totalMinutes);
        return response;
    }

    public int calculateDynamicBufferMinutes(Appointment previous, Appointment next, int configuredBufferMinutes) {
        if (previous == null || next == null || previous.isVirtual() || next.isVirtual()) {
            return configuredBufferMinutes;
        }
        if (previous.getClientLat() == null || previous.getClientLon() == null
                || next.getClientLat() == null || next.getClientLon() == null) {
            return configuredBufferMinutes;
        }
        GeoResponse.TravelTime travel = getTravelTime(previous.getClientLat(), previous.getClientLon(), next.getClientLat(), next.getClientLon());
        return Math.max(configuredBufferMinutes, travel.getDurationMinutes());
    }

    private Optional<GeoResponse.TravelTime> fetchTravelTimeFromOrs(double fromLat, double fromLng, double toLat, double toLng) {
        if (orsApiKey == null || orsApiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=" + orsApiKey
                    + "&start=" + fromLng + "," + fromLat
                    + "&end=" + toLng + "," + toLat;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                return Optional.empty();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(res.getBody());
            JsonNode summary = root.path("features").path(0).path("properties").path("summary");
            if (summary.isMissingNode()) {
                return Optional.empty();
            }

            double distanceKm = summary.path("distance").asDouble() / 1000.0;
            int durationMinutes = (int) Math.ceil(summary.path("duration").asDouble() / 60.0);

            GeoResponse.TravelTime response = new GeoResponse.TravelTime();
            response.setDistanceKm(BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP));
            response.setDurationMinutes(durationMinutes);
            response.setSummary("OpenRouteService route estimate");
            return Optional.of(response);
        } catch (Exception ex) {
            log.warn("OpenRouteService call failed, using fallback estimate: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private GeoResponse.TravelTime estimateFallback(double fromLat, double fromLng, double toLat, double toLng) {
        double distanceKm = haversineKm(fromLat, fromLng, toLat, toLng);
        int durationMinutes = (int) Math.max(1, Math.ceil((distanceKm / 30.0) * 60.0));

        GeoResponse.TravelTime response = new GeoResponse.TravelTime();
        response.setDistanceKm(BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP));
        response.setDurationMinutes(durationMinutes);
        response.setSummary("Estimated using fallback speed model");
        return response;
    }

    private String hashPoint(double lat, double lng) {
        String value = String.format("%.4f,%.4f", lat, lng);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return value;
        }
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
