package com.team27.lucky3.backend.controller;

import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    // Get current user profile
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getCurrentUser() {
        // ovo je sve mockovano. Tu bi trebalo nekako da on skonta ko je user, i da vrati odgovarajuce podatke iz baze
        // + da uradi vrv neke kalkuacije ako je potrebno npr pise kolko je user aktivan u poslednjih 24h, to radi service...

        UserProfile userProfileResponse = new UserProfile();
        userProfileResponse.setName("John");
        userProfileResponse.setSurname("Doe");
        userProfileResponse.setEmail("email@example.com");
        userProfileResponse.setPhoneNumber("123-456-7890");
        userProfileResponse.setAddress("123 Main St, Anytown, USA");
        // You can set vehicle information if needed
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setType("Toyota");
        vehicleInformation.setModel("Camry");
        vehicleInformation.setSeatsCount(5);
        vehicleInformation.setLicensePlateNumber("ABC-1234");
        vehicleInformation.setBabyTransportEnabled(Boolean.TRUE);
        vehicleInformation.setPetTransportEnabled(Boolean.FALSE);

        userProfileResponse.setVehicleInformation(vehicleInformation);

        return ResponseEntity.ok(userProfileResponse);
    }

    // Update user profile
    @PutMapping("/me")
    public ResponseEntity<UserProfile> updateCurrentUser(UserProfile userProfileEditRequest) {
        // ovde moram da pozivam service da iz repositorijuma izvuce usera, pa da update-ujem njegove podatke

        return ResponseEntity.ok(userProfileEditRequest);
    }

}
