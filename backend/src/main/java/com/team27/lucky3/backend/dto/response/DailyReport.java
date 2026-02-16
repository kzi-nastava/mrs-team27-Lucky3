package com.team27.lucky3.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyReport {
    private String date;
    private Integer rideCount;
    private Double kilometers;
    private Double money;
}
