package com.appointunified.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeakHoursDataPoint {
    private Integer hour;  // 0-23
    private Integer dayOfWeek;  // 0=Sunday, 6=Saturday
    private String dayName;  // 'Monday','Tuesday', etc.
    private Integer bookingCount;
    private Integer intensity;  // 0-100 for color intensity
}
