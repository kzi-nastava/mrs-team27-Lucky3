package com.team27.lucky3.backend.util;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.VehicleInformation;
import com.team27.lucky3.backend.dto.response.*;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.entity.enums.VehicleType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DummyData {

    public static RideResponse createDummyRideResponse(Long id, Long driverId, Long passengerId, RideStatus status) {
        LocationDto loc = new LocationDto("Bulevar oslobodjenja 10", 45.2464, 19.8517);
        RoutePointResponse p1 = new RoutePointResponse(new LocationDto("Bulevar oslobodjenja 10", 45.2464, 19.8517), 1);
        RoutePointResponse p2 = new RoutePointResponse(new LocationDto("Bulevar oslobodjenja 12", 45.2464, 19.8520), 2);

        UserResponse driver = new UserResponse(driverId, "DriverName", "DriverSurname", "driver" + driverId + "@example.com", "url", UserRole.DRIVER, "+381601234567");
        UserResponse passenger = new UserResponse(passengerId, "PassengerName", "PassengerSurname", "passenger" + passengerId + "@example.com", "url", UserRole.PASSENGER, "+381601234567");

        RideResponse response = new RideResponse();
        response.setId(id);
        response.setStatus(status);
        response.setDriver(driver);
        response.setPassengers(List.of(passenger));
        response.setStartTime(LocalDateTime.now().minusMinutes(10));
        response.setEndTime(status == RideStatus.FINISHED ? LocalDateTime.now() : null);
        response.setTotalCost(500.0);
        response.setVehicleType(VehicleType.STANDARD);
        response.setModel("Toyota Prius");
        response.setLicensePlates("NS-123-AB");
        response.setVehicleLocation(loc);
        response.setRoutePoints(List.of(p1, p2));
        response.setDistanceKm(3.5);
        response.setInconsistencyReports(new ArrayList<>());
        response.setEstimatedTimeInMinutes(5);
        response.setRejectionReason(null);
        response.setPanicPressed(status == RideStatus.PANIC);
        response.setDeparture(new LocationDto("Bulevar oslobodjenja 10", 45.2464, 19.8517));
        response.setDestination(new LocationDto("Bulevar oslobodjenja 12", 45.2464, 19.8520));
        response.setScheduledTime(null);
        response.setEstimatedCost(450.0);
        response.setPetTransport(true);
        response.setBabyTransport(true);
        response.setPaid(status == RideStatus.FINISHED);
        response.setPassengersExited(status == RideStatus.FINISHED);

        return response;
    }

    public static UserProfile createDummyUserProfile(Long id) {
        UserProfile profile = new UserProfile();
        profile.setName("User" + id);
        profile.setSurname("Surname" + id);
        profile.setEmail("user" + id + "@example.com");
        profile.setPhoneNumber("+38160123456" + id);
        profile.setAddress("Street " + id);
        profile.setImageUrl("default.png");
        
        // Dummy vehicle info for drivers
        VehicleInformation vehicle = new VehicleInformation();
        vehicle.setModel("Toyota Prius");
        vehicle.setVehicleType(VehicleType.STANDARD);
        vehicle.setLicenseNumber("NS-123-AB");
        vehicle.setPassengerSeats(4);
        vehicle.setBabyTransport(true);
        vehicle.setPetTransport(true);
        
        profile.setVehicleInformation(vehicle);
        profile.setActiveHours(5.5);
        
        return profile;
    }
}
