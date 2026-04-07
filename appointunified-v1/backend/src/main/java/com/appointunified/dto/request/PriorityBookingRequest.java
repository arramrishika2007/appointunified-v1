package com.appointunified.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriorityBookingRequest {
    private UUID serviceId;
    private UUID professionalId;
    private String appointmentDateTime;
    private String priorityTier;
    private String userNotes;
}
