package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.LocationDto;
import com.team27.lucky3.backend.dto.request.RideStopRequest;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.Location;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.Vehicle;
import com.team27.lucky3.backend.entity.enums.RideStatus;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.entity.enums.VehicleStatus;
import com.team27.lucky3.backend.entity.enums.VehicleType;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.*;
import com.team27.lucky3.backend.service.impl.RideServiceImpl;
import com.team27.lucky3.backend.service.socket.RideSocketService;
import com.team27.lucky3.backend.service.socket.VehicleSocketService;
import com.team27.lucky3.backend.util.ReviewTokenUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RideServiceImpl.stopRide() method.
 * Uses Mockito to mock all dependencies.
 * Tests all boundary and exceptional cases.
 */
@ExtendWith(MockitoExtension.class)
class RideServiceStopRideTest {

    @InjectMocks
    private RideServiceImpl rideService;

    @Mock
    private RideRepository rideRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FavoriteRouteRepository favoriteRouteRepository;
    @Mock
    private PanicRepository panicRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private InconsistencyReportRepository inconsistencyReportRepository;
    @Mock
    private DriverActivitySessionRepository activitySessionRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;
    @Mock
    private ReviewTokenUtils reviewTokenUtils;
    @Mock
    private PanicService panicService;
    @Mock
    private VehicleSocketService vehicleSocketService;
    @Mock
    private RideSocketService rideSocketService;
    @Mock
    private VehiclePriceService vehiclePriceService;

    private User driverUser;
    private User passengerUser;
    private User otherDriver;
    private Vehicle vehicle;
    private Ride inProgressRide;
    private RideStopRequest validStopRequest;

    @BeforeEach
    void setUp() {
        // Set up driver user
        driverUser = new User();
        driverUser.setId(1L);
        driverUser.setName("John");
        driverUser.setSurname("Driver");
        driverUser.setEmail("driver@example.com");
        driverUser.setRole(UserRole.DRIVER);
        driverUser.setActive(true);
        driverUser.setEnabled(true);

        // Set up passenger user
        passengerUser = new User();
        passengerUser.setId(2L);
        passengerUser.setName("Jane");
        passengerUser.setSurname("Passenger");
        passengerUser.setEmail("passenger@example.com");
        passengerUser.setRole(UserRole.PASSENGER);
        passengerUser.setEnabled(true);

        // Set up another driver (for testing authorization)
        otherDriver = new User();
        otherDriver.setId(3L);
        otherDriver.setName("Other");
        otherDriver.setSurname("Driver");
        otherDriver.setEmail("other.driver@example.com");
        otherDriver.setRole(UserRole.DRIVER);
        otherDriver.setActive(true);
        otherDriver.setEnabled(true);

        // Set up vehicle
        vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setModel("Toyota Camry");
        vehicle.setVehicleType(VehicleType.STANDARD);
        vehicle.setStatus(VehicleStatus.BUSY);
        vehicle.setLicensePlates("NS-123-AB");
        vehicle.setSeatCount(4);
        vehicle.setDriver(driverUser);
        vehicle.setCurrentPanic(false);
        vehicle.setCurrentLocation(new Location("Current", 45.2671, 19.8335));

        driverUser.setVehicle(vehicle);

        // Set up an IN_PROGRESS ride
        inProgressRide = new Ride();
        inProgressRide.setId(1L);
        inProgressRide.setDriver(driverUser);
        inProgressRide.setPassengers(Set.of(passengerUser));
        inProgressRide.setStatus(RideStatus.IN_PROGRESS);
        inProgressRide.setStartTime(LocalDateTime.now().minusMinutes(30));
        inProgressRide.setStartLocation(new Location("Bulevar Oslobodjenja 50", 45.2671, 19.8335));
        inProgressRide.setEndLocation(new Location("Trg Slobode 1", 45.2550, 19.8450));
        inProgressRide.setRequestedVehicleType(VehicleType.STANDARD);
        inProgressRide.setRateBaseFare(120.0);
        inProgressRide.setRatePricePerKm(120.0);
        inProgressRide.setDistance(5.0);
        inProgressRide.setEstimatedCost(720.0);
        inProgressRide.setPaid(false);
        inProgressRide.setPassengersExited(false);
        inProgressRide.setStops(null);
        inProgressRide.setInvitedEmails(null);

        // Set up a valid stop request
        validStopRequest = new RideStopRequest();
        validStopRequest.setStopLocation(new LocationDto("New Stop Address", 45.2600, 19.8400));
    }

    private void mockSecurityContext(User user) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Happy Path - Stop IN_PROGRESS ride
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - successfully stops an IN_PROGRESS ride")
    void stopRide_inProgressRide_success() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertNotNull(result);
        assertEquals(RideStatus.FINISHED, result.getStatus());
        assertTrue(result.getPaid());
        assertTrue(result.getPassengersExited());
        assertNotNull(result.getEndTime());
        assertNotNull(result.getDestination());
        assertEquals("New Stop Address", result.getDestination().getAddress());
    }

    @Test
    @DisplayName("stopRide - successfully stops an ACTIVE ride")
    void stopRide_activeRide_success() {
        inProgressRide.setStatus(RideStatus.ACTIVE);
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertNotNull(result);
        assertEquals(RideStatus.FINISHED, result.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Ride Status Validation
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest
    @EnumSource(value = RideStatus.class, names = {"PENDING", "ACCEPTED", "SCHEDULED", "FINISHED",
            "REJECTED", "PANIC", "CANCELLED", "CANCELLED_BY_DRIVER", "CANCELLED_BY_PASSENGER"})
    @DisplayName("stopRide - throws IllegalStateException for non-IN_PROGRESS/ACTIVE statuses")
    void stopRide_invalidStatus_throwsIllegalState(RideStatus status) {
        inProgressRide.setStatus(status);
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> rideService.stopRide(1L, validStopRequest));
        assertTrue(ex.getMessage().contains("not in progress"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Authentication Validation
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - throws IllegalStateException when user is not authenticated")
    void stopRide_notAuthenticated_throwsIllegalState() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> rideService.stopRide(1L, validStopRequest));
        assertTrue(ex.getMessage().contains("authenticated"));
    }

    @Test
    @DisplayName("stopRide - throws IllegalStateException when user is not the assigned driver")
    void stopRide_wrongDriver_throwsIllegalState() {
        mockSecurityContext(otherDriver);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> rideService.stopRide(1L, validStopRequest));
        assertTrue(ex.getMessage().contains("assigned driver"));
    }

    @Test
    @DisplayName("stopRide - throws IllegalStateException when passenger tries to stop ride")
    void stopRide_passengerTriesToStop_throwsIllegalState() {
        mockSecurityContext(passengerUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> rideService.stopRide(1L, validStopRequest));
        assertTrue(ex.getMessage().contains("assigned driver"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Ride Not Found
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - throws ResourceNotFoundException when ride does not exist")
    void stopRide_rideNotFound_throwsResourceNotFound() {
        when(rideRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> rideService.stopRide(999L, validStopRequest));
        assertTrue(ex.getMessage().contains("Ride not found"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Ride with No Driver Assigned
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - throws IllegalStateException when ride has no driver assigned")
    void stopRide_noDriverAssigned_throwsIllegalState() {
        inProgressRide.setDriver(null);
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> rideService.stopRide(1L, validStopRequest));
        assertTrue(ex.getMessage().contains("assigned driver"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  End Location Update
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - updates end location to the stop location from request")
    void stopRide_updatesEndLocation() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride saved = rideCaptor.getValue();

        assertEquals("New Stop Address", saved.getEndLocation().getAddress());
        assertEquals(45.2600, saved.getEndLocation().getLatitude());
        assertEquals(19.8400, saved.getEndLocation().getLongitude());
    }

    // ═══════════════════════════════════════════════════════════════
    //  End Time is Set
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - sets endTime to approximately now")
    void stopRide_setsEndTime() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        LocalDateTime before = LocalDateTime.now();
        rideService.stopRide(1L, validStopRequest);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride saved = rideCaptor.getValue();

        assertFalse(saved.getEndTime().isBefore(before));
        assertFalse(saved.getEndTime().isAfter(after));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Status is Set to FINISHED
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - sets status to FINISHED")
    void stopRide_setsStatusToFinished() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());

        assertEquals(RideStatus.FINISHED, rideCaptor.getValue().getStatus());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Distance Recalculation
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - uses tracked distance from RideCostTrackingService (no stops)")
    void stopRide_recalculatesDistance_noStops() {
        inProgressRide.setDistanceTraveled(3.5);

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride saved = rideCaptor.getValue();

        // Distance should equal the tracked distance
        assertTrue(saved.getDistance() > 0.0);
        assertEquals(3.5, saved.getDistance());
    }

    @Test
    @DisplayName("stopRide - uses tracked distance even with intermediate stops")
    void stopRide_recalculatesDistance_withStops() {
        List<Location> stops = List.of(
                new Location("Stop 1", 45.2630, 19.8360),
                new Location("Stop 2", 45.2610, 19.8380)
        );
        inProgressRide.setStops(stops);
        inProgressRide.setDistanceTraveled(5.2);

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride saved = rideCaptor.getValue();

        // Distance should equal the tracked distance
        assertTrue(saved.getDistance() > 0.0);
        assertEquals(5.2, saved.getDistance());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Cost Recalculation
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - recalculates cost using snapshotted rates")
    void stopRide_recalculatesCost_usingSnapshotRates() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride saved = rideCaptor.getValue();

        // totalCost = 120 (base) + distance * 120 (perKm)
        double expectedCost = Math.round((120.0 + (saved.getDistance() * 120.0)) * 100.0) / 100.0;
        assertEquals(expectedCost, saved.getTotalCost());
    }

    @Test
    @DisplayName("stopRide - falls back to VehiclePriceService when rates are null")
    void stopRide_fallsBackToVehiclePriceService_whenRatesNull() {
        inProgressRide.setRateBaseFare(null);
        inProgressRide.setRatePricePerKm(null);

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());
        when(vehiclePriceService.getBaseFare(VehicleType.STANDARD)).thenReturn(120.0);
        when(vehiclePriceService.getPricePerKm(VehicleType.STANDARD)).thenReturn(120.0);

        rideService.stopRide(1L, validStopRequest);

        verify(vehiclePriceService).getBaseFare(VehicleType.STANDARD);
        verify(vehiclePriceService).getPricePerKm(VehicleType.STANDARD);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());

        assertTrue(rideCaptor.getValue().getTotalCost() > 0.0);
    }

    @Test
    @DisplayName("stopRide - cost is rounded to 2 decimal places")
    void stopRide_costIsRoundedTo2DecimalPlaces() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());

        String costStr = String.valueOf(rideCaptor.getValue().getTotalCost());
        int decimalIndex = costStr.indexOf('.');
        if (decimalIndex >= 0) {
            int decimalPlaces = costStr.length() - decimalIndex - 1;
            assertTrue(decimalPlaces <= 2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Flags: passengersExited and paid
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - sets passengersExited to true and paid to true")
    void stopRide_setsFlags() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride saved = rideCaptor.getValue();

        assertTrue(saved.isPassengersExited());
        assertTrue(saved.isPaid());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Notifications
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - triggers ride finished notification")
    void stopRide_triggersRideFinishedNotification() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        verify(notificationService).sendRideFinishedNotification(any(Ride.class));
    }

    @Test
    @DisplayName("stopRide - notifies linked passengers about ride completion")
    void stopRide_notifiesLinkedPassengers() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        verify(notificationService).notifyLinkedPassengersRideCompleted(any(Ride.class));
    }

    @Test
    @DisplayName("stopRide - broadcasts ride update via WebSocket")
    void stopRide_broadcastsRideUpdate() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        verify(rideSocketService).broadcastRideUpdate(eq(1L), any(RideResponse.class));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Vehicle Status Update
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - sets vehicle status to FREE when no next rides")
    void stopRide_setsVehicleToFree_whenNoNextRides() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());
        Vehicle savedVehicle = vehicleCaptor.getValue();

        assertEquals(VehicleStatus.FREE, savedVehicle.getStatus());
        assertFalse(savedVehicle.isCurrentPanic());
    }

    @Test
    @DisplayName("stopRide - sets vehicle status to BUSY when driver has next scheduled rides")
    void stopRide_setsVehicleToBusy_whenNextRidesExist() {
        Ride nextRide = new Ride();
        nextRide.setId(2L);
        nextRide.setStatus(RideStatus.SCHEDULED);

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(List.of(nextRide));

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());

        assertEquals(VehicleStatus.BUSY, vehicleCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("stopRide - resets vehicle panic flag to false")
    void stopRide_resetsPanicFlag() {
        vehicle.setCurrentPanic(true);
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());

        assertFalse(vehicleCaptor.getValue().isCurrentPanic());
    }

    @Test
    @DisplayName("stopRide - handles gracefully when vehicle not found for driver")
    void stopRide_vehicleNotFound_handlesGracefully() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.empty());
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        // Should not throw even if vehicle is not found
        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertNotNull(result);
        assertEquals(RideStatus.FINISHED, result.getStatus());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Ride is Saved Correctly
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - saves the ride entity exactly once")
    void stopRide_savesRideOnce() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Inactive Driver Check
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - deactivates driver if inactiveRequested and no more rides")
    void stopRide_deactivatesDriver_ifInactiveRequested() {
        driverUser.setInactiveRequested(true);

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverUser.getId()),
                eq(List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS))))
                .thenReturn(false);
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverUser.getId()),
                eq(List.of(RideStatus.SCHEDULED, RideStatus.PENDING))))
                .thenReturn(false);
        when(activitySessionRepository.findByDriverIdAndEndTimeIsNull(driverUser.getId()))
                .thenReturn(Optional.empty());

        rideService.stopRide(1L, validStopRequest);

        assertFalse(driverUser.isActive());
        assertFalse(driverUser.isInactiveRequested());
        verify(userRepository).save(driverUser);
    }

    @Test
    @DisplayName("stopRide - keeps driver active if inactiveRequested but has more rides")
    void stopRide_keepsDriverActive_ifMoreRidesExist() {
        driverUser.setInactiveRequested(true);

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverUser.getId()),
                eq(List.of(RideStatus.ACCEPTED, RideStatus.ACTIVE, RideStatus.IN_PROGRESS))))
                .thenReturn(true);

        rideService.stopRide(1L, validStopRequest);

        assertTrue(driverUser.isActive());
        verify(userRepository, never()).save(driverUser);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Edge Cases: Stops with same start and end
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - handles stop at exact start location (distance = 0)")
    void stopRide_stopAtStartLocation_distanceZero() {
        validStopRequest.setStopLocation(new LocationDto("Same as start", 45.2671, 19.8335));

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride saved = rideCaptor.getValue();

        // Distance should be 0 or very close to 0
        assertTrue(saved.getDistance() <= 0.01);
        // Cost should be just the base fare
        assertEquals(120.0, saved.getTotalCost());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Edge Cases: LUXURY and VAN vehicle types
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - uses LUXURY rates when ride has LUXURY vehicle type")
    void stopRide_luxuryVehicleType_usesLuxuryRates() {
        inProgressRide.setRequestedVehicleType(VehicleType.LUXURY);
        inProgressRide.setRateBaseFare(360.0);
        inProgressRide.setRatePricePerKm(120.0);
        inProgressRide.setDistanceTraveled(2.0);

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.stopRide(1L, validStopRequest);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        // Base fare should be 360 (LUXURY) not 120 (STANDARD)
        assertTrue(rideCaptor.getValue().getTotalCost() > 360.0);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Response DTO Mapping
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - response contains driver information")
    void stopRide_responseContainsDriverInfo() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertNotNull(result.getDriver());
        assertEquals(driverUser.getId(), result.getDriver().getId());
        assertEquals("John", result.getDriver().getName());
    }

    @Test
    @DisplayName("stopRide - response contains passenger information")
    void stopRide_responseContainsPassengerInfo() {
        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertNotNull(result.getPassengers());
        assertEquals(1, result.getPassengers().size());
        assertEquals("passenger@example.com", result.getPassengers().get(0).getEmail());
    }

    @Test
    @DisplayName("stopRide - response contains recalculated cost and distance")
    void stopRide_responseContainsRecalculatedValues() {
        inProgressRide.setDistanceTraveled(3.0);

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertNotNull(result.getTotalCost());
        assertTrue(result.getTotalCost() > 0.0);
        assertNotNull(result.getDistanceKm());
        assertTrue(result.getDistanceKm() > 0.0);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Ride with Empty Stops List
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - handles ride with empty stops list")
    void stopRide_emptyStopsList_success() {
        inProgressRide.setStops(new ArrayList<>());

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertNotNull(result);
        assertEquals(RideStatus.FINISHED, result.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Boundary: Stop location at geographic extremes
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("stopRide - handles stop location at maximum latitude boundary (90)")
    void stopRide_maxLatitude_success() {
        validStopRequest.setStopLocation(new LocationDto("North Pole", 90.0, 0.0));

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertEquals(RideStatus.FINISHED, result.getStatus());
    }

    @Test
    @DisplayName("stopRide - handles stop location at minimum latitude boundary (-90)")
    void stopRide_minLatitude_success() {
        validStopRequest.setStopLocation(new LocationDto("South Pole", -90.0, 0.0));

        mockSecurityContext(driverUser);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse result = rideService.stopRide(1L, validStopRequest);

        assertEquals(RideStatus.FINISHED, result.getStatus());
    }
}
