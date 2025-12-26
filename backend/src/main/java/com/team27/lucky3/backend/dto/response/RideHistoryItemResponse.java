package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.entity.enums.RideStatus;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideHistoryItemResponse {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String pickupAddress;
    private String destinationAddress;
    private double price;
    private RideStatus status;
    private String canceledBy;
    private boolean panicTriggered;
}
