package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverChangeRequestCreated {
    public Long driverId;
    public Long changeRequestId;
    public DriverChangeStatus status;
}
