package com.appointunified.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class GeoResponse {

    public static class TravelTime {
        private BigDecimal distanceKm;
        private Integer durationMinutes;
        private String summary;

        public BigDecimal getDistanceKm() { return distanceKm; }
        public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    public static class RouteLeg {
        private UUID fromAppointmentId;
        private UUID toAppointmentId;
        private String fromClient;
        private String toClient;
        private OffsetDateTime fromStartTime;
        private OffsetDateTime toStartTime;
        private BigDecimal distanceKm;
        private Integer durationMinutes;

        public UUID getFromAppointmentId() { return fromAppointmentId; }
        public void setFromAppointmentId(UUID fromAppointmentId) { this.fromAppointmentId = fromAppointmentId; }
        public UUID getToAppointmentId() { return toAppointmentId; }
        public void setToAppointmentId(UUID toAppointmentId) { this.toAppointmentId = toAppointmentId; }
        public String getFromClient() { return fromClient; }
        public void setFromClient(String fromClient) { this.fromClient = fromClient; }
        public String getToClient() { return toClient; }
        public void setToClient(String toClient) { this.toClient = toClient; }
        public OffsetDateTime getFromStartTime() { return fromStartTime; }
        public void setFromStartTime(OffsetDateTime fromStartTime) { this.fromStartTime = fromStartTime; }
        public OffsetDateTime getToStartTime() { return toStartTime; }
        public void setToStartTime(OffsetDateTime toStartTime) { this.toStartTime = toStartTime; }
        public BigDecimal getDistanceKm() { return distanceKm; }
        public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    }

    public static class RouteOptimize {
        private UUID professionalId;
        private OffsetDateTime dateStart;
        private Integer offlineAppointments;
        private BigDecimal totalDistanceKm;
        private Integer totalTravelMinutes;
        private List<RouteLeg> legs;

        public UUID getProfessionalId() { return professionalId; }
        public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }
        public OffsetDateTime getDateStart() { return dateStart; }
        public void setDateStart(OffsetDateTime dateStart) { this.dateStart = dateStart; }
        public Integer getOfflineAppointments() { return offlineAppointments; }
        public void setOfflineAppointments(Integer offlineAppointments) { this.offlineAppointments = offlineAppointments; }
        public BigDecimal getTotalDistanceKm() { return totalDistanceKm; }
        public void setTotalDistanceKm(BigDecimal totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }
        public Integer getTotalTravelMinutes() { return totalTravelMinutes; }
        public void setTotalTravelMinutes(Integer totalTravelMinutes) { this.totalTravelMinutes = totalTravelMinutes; }
        public List<RouteLeg> getLegs() { return legs; }
        public void setLegs(List<RouteLeg> legs) { this.legs = legs; }
    }
}
