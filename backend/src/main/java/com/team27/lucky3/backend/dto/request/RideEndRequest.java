package com.team27.lucky3.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideEndRequest {
    private boolean paid;
    private boolean finished;
}
