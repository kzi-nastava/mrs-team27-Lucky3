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

        VehicleInformation vehicle = createDummyVehicle(driverId);
        DriverResponse driver = new DriverResponse(driverId, "DriverName", "DriverSurname", "driver" + driverId + "@example.com", "url", UserRole.DRIVER, "+381601234567", "Driver Address", vehicle, true, false, "5h 30m");
        UserResponse passenger = new UserResponse(passengerId, "PassengerName", "PassengerSurname", "passenger" + passengerId + "@example.com", "url", UserRole.PASSENGER, "+381601234567", "Passenger Address");

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
        return new UserProfile(
                "Name" + id,           // name
                "Surname" + id,        // surname
                "user" + id + "@example.com", // email
                "+381601234567",       // phoneNumber
                "Address " + id,       // address
                "profile.jpg"         // imageUrl
        );
    }

    public static VehicleInformation createDummyVehicle(Long driverId) {
        return new VehicleInformation("Toyota Prius", VehicleType.STANDARD, "NS-123-AB", 4, true, true, driverId);
    }

    public static FavoriteRouteResponse createDummyFavoriteRoute(Long id) {
        LocationDto start = new LocationDto("Start Address", 45.2464, 19.8517);
        LocationDto end = new LocationDto("End Address", 45.2564, 19.8617);
        return new FavoriteRouteResponse(id, "Home to Work", start, end, new ArrayList<>(), 5.0, 15.0);
    }

    public static List<DriverResponse> createSampleDrivers() {
        List<DriverResponse> drivers = new ArrayList<>();

        // Driver 1 - Marko Jovanović
        VehicleInformation vehicle1 = new VehicleInformation(
                "Volkswagen Passat",
                VehicleType.STANDARD,
                "NS-123-AB",
                4,
                true,
                false,
                1L
        );
        drivers.add(new DriverResponse(
                1L,
                "Marko",
                "Jovanović",
                "marko.jovanovic@gmail.com",
                "https://example.com/profiles/marko.jpg",
                UserRole.DRIVER,
                "+381 63 456 7890",
                "Bulevar Oslobođenja 45, Novi Sad",
                vehicle1,
                true,
                true,
                "8h"
        ));

        // Driver 2 - Ana Petrović
        VehicleInformation vehicle2 = new VehicleInformation(
                "Škoda Octavia",
                VehicleType.STANDARD,
                "NS-456-CD",
                4,
                true,
                true,
                2L
        );
        drivers.add(new DriverResponse(
                2L,
                "Ana",
                "Petrović",
                "ana.petrovic@yahoo.com",
                "https://example.com/profiles/ana.jpg",
                UserRole.DRIVER,
                "+381 64 789 1234",
                "Futoška ulica 23, Novi Sad",
                vehicle2,
                true,
                false,
                "12h"
        ));

        // Driver 3 - Nikola Đorđević
        VehicleInformation vehicle3 = new VehicleInformation(
                "Toyota Corolla",
                VehicleType.STANDARD,
                "NS-789-EF",
                4,
                false,
                false,
                3L
        );
        drivers.add(new DriverResponse(
                3L,
                "Nikola",
                "Đorđević",
                "nikola.djordjevic@gmail.com",
                "https://example.com/profiles/nikola.jpg",
                UserRole.DRIVER,
                "+381 65 321 9876",
                "Narodnih Heroja 12, Novi Sad",
                vehicle3,
                false,
                false,
                "2h"
        ));

        // Driver 4 - Milica Nikolić
        VehicleInformation vehicle4 = new VehicleInformation(
                "Renault Clio",
                VehicleType.STANDARD,
                "NS-234-GH",
                4,
                true,
                true,
                4L
        );
        drivers.add(new DriverResponse(
                4L,
                "Milica",
                "Nikolić",
                "milica.nikolic@outlook.com",
                "https://example.com/profiles/milica.jpg",
                UserRole.DRIVER,
                "+381 66 654 3210",
                "Kisačka ulica 56, Novi Sad",
                vehicle4,
                true,
                true,
                "15h"
        ));

        // Driver 5 - Stefan Marković
        VehicleInformation vehicle5 = new VehicleInformation(
                "Peugeot 308",
                VehicleType.STANDARD,
                "NS-567-IJ",
                4,
                false,
                true,
                5L
        );
        drivers.add(new DriverResponse(
                5L,
                "Stefan",
                "Marković",
                "stefan.markovic@gmail.com",
                "https://example.com/profiles/stefan.jpg",
                UserRole.DRIVER,
                "+381 69 987 6543",
                "Radnička ulica 34, Novi Sad",
                vehicle5,
                false,
                false,
                "0h"
        ));

        return drivers;
    }
}
