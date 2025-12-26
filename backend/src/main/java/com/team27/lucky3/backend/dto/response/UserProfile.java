package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.dto.request.VehicleInformation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class UserProfile {
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private String address;
    private String imageUrl;
    private VehicleInformation vehicleInformation;
}

