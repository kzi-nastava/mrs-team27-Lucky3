package com.team27.lucky3.backend.entity;


import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.entity.enums.DriverChangeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "driver_change_requests")
public class DriverChangeRequest {

    @Id
    @GeneratedValue
    private Long id;

    //private Long driverId;              // which driver is changing it should be linked from vehicle information

    private String name;
    private String surname;
    private String email;
    private String address;
    private String phone;
    private Long driverRequestId;
    private LocalDateTime createdAt;

    private Long imageId; // or just store imageId if preferred

    @Embedded
    private VehicleInformation vehicle; // or a separate embedded / entity

    @Enumerated(EnumType.STRING)
    private DriverChangeStatus status = DriverChangeStatus.PENDING;
}
