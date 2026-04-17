package com.appointunified.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsResponse {
    private long totalProfessionals;
    private long totalUsers;
    private long totalAppointments;
    private List<MapPin> mapPins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapPin {
        private UUID id;
        private String displayName;
        private String sector;
        private String specialty;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}
