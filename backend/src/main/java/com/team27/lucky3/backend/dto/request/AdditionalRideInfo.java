package com.team27.lucky3.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AdditionalRideInfo {
    private boolean babySeats;
    private boolean petFriendly;
}
