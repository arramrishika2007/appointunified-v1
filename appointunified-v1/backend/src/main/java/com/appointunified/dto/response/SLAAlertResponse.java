package com.appointunified.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SLAAlertResponse {
    private UUID eventId;
    private UUID appointmentId;
    private String breachType;
    private LocalDateTime breachedAt;
    private LocalDateTime resolvedAt;
    private UUID escalatedTo;
    private String status;
    private Long minutesOverdue;
}
