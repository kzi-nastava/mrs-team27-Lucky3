package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.request.EndRideRequest;
import com.team27.lucky3.backend.dto.response.RideResponse;
import com.team27.lucky3.backend.entity.*;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceEndRideTest {

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
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    private User driverUser;
    private User passengerUser;
    private Vehicle vehicle;
    private Ride inProgressRide;
    private EndRideRequest validEndRequest;

    @BeforeEach
    void setUp() {
        driverUser = new User();
        driverUser.setId(1L);
        driverUser.setEmail("driver@example.com");
        driverUser.setRole(UserRole.DRIVER);

        passengerUser = new User();
        passengerUser.setId(2L);
        passengerUser.setEmail("passenger@example.com");
        passengerUser.setRole(UserRole.PASSENGER);

        vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setDriver(driverUser);
        vehicle.setStatus(VehicleStatus.BUSY);
        vehicle.setCurrentPanic(true);

        inProgressRide = new Ride();
        inProgressRide.setId(1L);
        inProgressRide.setStatus(RideStatus.IN_PROGRESS);
        inProgressRide.setDriver(driverUser);
        inProgressRide.setPassengers(Set.of(passengerUser));

        validEndRequest = new EndRideRequest();
        validEndRequest.setPaid(true);
        validEndRequest.setPassengersExited(true);
    }

    @Test
    @DisplayName("endRide - success when status is IN_PROGRESS")
    void endRide_inProgressRide_success() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse response = rideService.endRide(1L, validEndRequest);

        assertNotNull(response);
        assertEquals(RideStatus.FINISHED, inProgressRide.getStatus());
        assertNotNull(inProgressRide.getEndTime());
        assertTrue(inProgressRide.isPaid());
        assertTrue(inProgressRide.isPassengersExited());
        assertEquals(VehicleStatus.FREE, vehicle.getStatus());
        assertFalse(vehicle.isCurrentPanic());
        
        verify(rideRepository).save(inProgressRide);
        verify(notificationService).sendRideFinishedNotification(any());
        verify(notificationService).notifyLinkedPassengersRideCompleted(any());
        verify(rideSocketService).broadcastRideUpdate(eq(1L), any());
    }

    @Test
    @DisplayName("endRide - success when status is ACTIVE")
    void endRide_activeRide_success() {
        inProgressRide.setStatus(RideStatus.ACTIVE);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        
        rideService.endRide(1L, validEndRequest);

        assertEquals(RideStatus.FINISHED, inProgressRide.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = RideStatus.class, names = {"PENDING", "ACCEPTED", "SCHEDULED", "FINISHED", "CANCELLED", "CANCELLED_BY_DRIVER", "CANCELLED_BY_PASSENGER", "REJECTED", "PANIC"})
    @DisplayName("endRide - throws IllegalStateException for invalid statuses")
    void endRide_invalidStatus_throwsIllegalState(RideStatus status) {
        inProgressRide.setStatus(status);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));

        assertThrows(IllegalStateException.class, () -> rideService.endRide(1L, validEndRequest));
    }

    @Test
    @DisplayName("endRide - throws ResourceNotFoundException when ride not found")
    void endRide_rideNotFound_throwsResourceNotFound() {
        when(rideRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rideService.endRide(999L, validEndRequest));
    }

    @Test
    @DisplayName("endRide - sets vehicle to BUSY when next rides exist")
    void endRide_setsVehicleToBusy_whenNextRidesExist() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(List.of(new Ride()));

        rideService.endRide(1L, validEndRequest);

        assertEquals(VehicleStatus.BUSY, vehicle.getStatus());
    }

    @Test
    @DisplayName("endRide - handles null driver gracefully")
    void endRide_noDriver_skipsVehicleUpdate() {
        inProgressRide.setDriver(null);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> rideService.endRide(1L, validEndRequest));
        verify(vehicleRepository, never()).findByDriverId(anyLong());
    }

    @Test
    @DisplayName("endRide - deactivates driver when inactive requested and no more rides")
    void endRide_deactivatesDriver_ifInactiveRequested() {
        driverUser.setInactiveRequested(true);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());
        
        // checkAndHandleInactiveRequest mocks
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverUser.getId()), anyList()))
                .thenReturn(false); // No active/upcoming rides
        when(activitySessionRepository.findByDriverIdAndEndTimeIsNull(driverUser.getId()))
                .thenReturn(Optional.of(new DriverActivitySession()));

        rideService.endRide(1L, validEndRequest);

        assertFalse(driverUser.isActive());
        assertFalse(driverUser.isInactiveRequested());
        verify(userRepository).save(driverUser);
        verify(activitySessionRepository).save(any(DriverActivitySession.class));
    }

    @Test
    @DisplayName("endRide - keeps driver active when inactive requested but has upcoming rides")
    void endRide_keepsDriverActive_ifMoreRidesExist() {
        driverUser.setInactiveRequested(true);
        driverUser.setActive(true);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        
        // Mock that there are upcoming rides
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverUser.getId()), anyList()))
                .thenReturn(true);

        rideService.endRide(1L, validEndRequest);

        assertTrue(driverUser.isActive());
        assertTrue(driverUser.isInactiveRequested());
        verify(userRepository, never()).save(driverUser);
    }

    @Test
    @DisplayName("endRide - sends review request email to ride creator")
    void endRide_sendsReviewEmails_toRealPassengers() {
        passengerUser.setEmail("real@gmail.com");
        passengerUser.setName("Real Passenger");

        // Set createdBy so the implementation sends to the creator
        inProgressRide.setCreatedBy(passengerUser);
        inProgressRide.setPassengers(Set.of(passengerUser));

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(reviewTokenUtils.generateReviewToken(anyLong(), anyLong(), anyLong())).thenReturn("token1");

        rideService.endRide(1L, validEndRequest);

        // Only the ride creator receives the review request email
        verify(emailService, times(1)).sendReviewRequestEmail(eq("real@gmail.com"), eq("Real Passenger"), eq("token1"));
    }

    @Test
    @DisplayName("endRide - skips review emails for @example.com addresses")
    void endRide_skipsReviewEmails_forExampleAddresses() {
        passengerUser.setEmail("test@example.com");
        inProgressRide.setInvitedEmails(List.of("someone@example.com"));

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));

        rideService.endRide(1L, validEndRequest);

        verify(emailService, never()).sendReviewRequestEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("endRide - sets vehicle to FREE when no next rides exist")
    void endRide_setsVehicleToFree_whenNoNextRides() {
        vehicle.setStatus(VehicleStatus.BUSY);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.endRide(1L, validEndRequest);

        assertEquals(VehicleStatus.FREE, vehicle.getStatus());
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @DisplayName("endRide - resets vehicle panic flag to false")
    void endRide_resetsPanicFlag() {
        vehicle.setCurrentPanic(true);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.endRide(1L, validEndRequest);

        assertFalse(vehicle.isCurrentPanic());
    }

    @Test
    @DisplayName("endRide - handles vehicle not found gracefully (no vehicleRepository.save)")
    void endRide_vehicleNotFound_handlesGracefully() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.empty());
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse response = rideService.endRide(1L, validEndRequest);

        assertNotNull(response);
        assertEquals(RideStatus.FINISHED, response.getStatus());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("endRide - sets paid=false and passengersExited=false from request")
    void endRide_setsFlagsFalse_fromRequest() {
        EndRideRequest falseRequest = new EndRideRequest();
        falseRequest.setPaid(false);
        falseRequest.setPassengersExited(false);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.endRide(1L, falseRequest);

        assertFalse(inProgressRide.isPaid());
        assertFalse(inProgressRide.isPassengersExited());
    }

    @Test
    @DisplayName("endRide - sets endTime to approximately now")
    void endRide_setsEndTime() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        LocalDateTime before = LocalDateTime.now();
        rideService.endRide(1L, validEndRequest);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(inProgressRide.getEndTime());
        assertFalse(inProgressRide.getEndTime().isBefore(before));
        assertFalse(inProgressRide.getEndTime().isAfter(after));
    }

    @Test
    @DisplayName("endRide - saves ride exactly once")
    void endRide_savesRideOnce() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.endRide(1L, validEndRequest);

        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    @DisplayName("endRide - saves vehicle when present")
    void endRide_savesVehicle_whenPresent() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.endRide(1L, validEndRequest);

        verify(vehicleRepository, times(1)).save(vehicle);
    }

    @Test
    @DisplayName("endRide - vehicle stays BUSY when next PENDING ride exists")
    void endRide_vehicleStaysBusy_whenPendingRideExists() {
        Ride pendingRide = new Ride();
        pendingRide.setId(2L);
        pendingRide.setStatus(RideStatus.PENDING);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(List.of(pendingRide));

        rideService.endRide(1L, validEndRequest);

        assertEquals(VehicleStatus.BUSY, vehicle.getStatus());
    }

    @Test
    @DisplayName("endRide - vehicle stays BUSY when multiple next rides exist")
    void endRide_vehicleStaysBusy_whenMultipleNextRidesExist() {
        Ride scheduledRide = new Ride();
        scheduledRide.setId(2L);
        scheduledRide.setStatus(RideStatus.SCHEDULED);
        Ride pendingRide = new Ride();
        pendingRide.setId(3L);
        pendingRide.setStatus(RideStatus.PENDING);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(List.of(scheduledRide, pendingRide));

        rideService.endRide(1L, validEndRequest);

        assertEquals(VehicleStatus.BUSY, vehicle.getStatus());
    }

    @Test
    @DisplayName("endRide - response contains all required fields")
    void endRide_responseFieldVerification() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        
        RideResponse response = rideService.endRide(1L, validEndRequest);

        assertNotNull(response.getId());
        assertEquals(RideStatus.FINISHED, response.getStatus());
        assertNotNull(response.getDriver());
        assertEquals(driverUser.getEmail(), response.getDriver().getEmail());
        assertNotNull(response.getPassengers());
        assertEquals(1, response.getPassengers().size());
        assertTrue(response.getPaid());
        assertTrue(response.getPassengersExited());
    }

    // ═══════════════════════════════════════════════════════════════
    //  sendReviewRequestEmails edge cases
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("endRide - falls back to first passenger when createdBy is null")
    void endRide_fallbackToFirstPassenger_whenCreatedByNull() {
        passengerUser.setEmail("fallback@gmail.com");
        passengerUser.setName("Fallback Passenger");
        inProgressRide.setCreatedBy(null);
        inProgressRide.setPassengers(Set.of(passengerUser));

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(reviewTokenUtils.generateReviewToken(anyLong(), anyLong(), anyLong())).thenReturn("token-fb");

        rideService.endRide(1L, validEndRequest);

        verify(emailService, times(1)).sendReviewRequestEmail(
                eq("fallback@gmail.com"), eq("Fallback Passenger"), eq("token-fb"));
    }

    @Test
    @DisplayName("endRide - uses 'Valued Customer' when creator name is null")
    void endRide_valuedCustomer_whenCreatorNameNull() {
        passengerUser.setEmail("noname@gmail.com");
        passengerUser.setName(null);
        inProgressRide.setCreatedBy(passengerUser);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(reviewTokenUtils.generateReviewToken(anyLong(), anyLong(), anyLong())).thenReturn("token-vc");

        rideService.endRide(1L, validEndRequest);

        verify(emailService).sendReviewRequestEmail(
                eq("noname@gmail.com"), eq("Valued Customer"), eq("token-vc"));
    }

    @Test
    @DisplayName("endRide - uses 'Valued Customer' when creator name is blank")
    void endRide_valuedCustomer_whenCreatorNameBlank() {
        passengerUser.setEmail("blank@gmail.com");
        passengerUser.setName("   ");
        inProgressRide.setCreatedBy(passengerUser);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(reviewTokenUtils.generateReviewToken(anyLong(), anyLong(), anyLong())).thenReturn("token-bl");

        rideService.endRide(1L, validEndRequest);

        verify(emailService).sendReviewRequestEmail(
                eq("blank@gmail.com"), eq("Valued Customer"), eq("token-bl"));
    }

    @Test
    @DisplayName("endRide - skips review email when creator email is null")
    void endRide_skipsReviewEmail_whenCreatorEmailNull() {
        passengerUser.setEmail(null);
        inProgressRide.setCreatedBy(passengerUser);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));

        rideService.endRide(1L, validEndRequest);

        verify(emailService, never()).sendReviewRequestEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("endRide - handles emailService exception gracefully")
    void endRide_emailServiceException_handledGracefully() {
        passengerUser.setEmail("fail@gmail.com");
        passengerUser.setName("Fail User");
        inProgressRide.setCreatedBy(passengerUser);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(reviewTokenUtils.generateReviewToken(anyLong(), anyLong(), anyLong())).thenReturn("token-fail");
        doThrow(new RuntimeException("SMTP error")).when(emailService)
                .sendReviewRequestEmail(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> rideService.endRide(1L, validEndRequest));
        assertEquals(RideStatus.FINISHED, inProgressRide.getStatus());
    }

    @Test
    @DisplayName("endRide - skips review emails when ride has no passengers and no createdBy")
    void endRide_skipsReviewEmails_noPassengersNoCreatedBy() {
        inProgressRide.setCreatedBy(null);
        inProgressRide.setPassengers(Collections.emptySet());

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));

        rideService.endRide(1L, validEndRequest);

        verify(emailService, never()).sendReviewRequestEmail(anyString(), anyString(), anyString());
    }

    // ═══════════════════════════════════════════════════════════════
    //  checkAndHandleInactiveRequest edge cases
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("endRide - does not trigger deactivation when inactiveRequested is false")
    void endRide_noDeactivation_whenInactiveNotRequested() {
        driverUser.setInactiveRequested(false);
        driverUser.setActive(true);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.endRide(1L, validEndRequest);

        assertTrue(driverUser.isActive());
        verify(userRepository, never()).save(any(User.class));
        verify(rideRepository, never()).existsByDriverIdAndStatusIn(anyLong(), anyList());
    }

    @Test
    @DisplayName("endRide - deactivates driver even when no activity session exists")
    void endRide_deactivatesDriver_noActivitySession() {
        driverUser.setInactiveRequested(true);
        driverUser.setActive(true);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverUser.getId()), anyList()))
                .thenReturn(false);
        when(activitySessionRepository.findByDriverIdAndEndTimeIsNull(driverUser.getId()))
                .thenReturn(Optional.empty());

        rideService.endRide(1L, validEndRequest);

        assertFalse(driverUser.isActive());
        assertFalse(driverUser.isInactiveRequested());
        verify(userRepository).save(driverUser);
        verify(activitySessionRepository, never()).save(any(DriverActivitySession.class));
    }

    @Test
    @DisplayName("endRide - keeps driver active when has active rides (IN_PROGRESS)")
    void endRide_keepsDriverActive_ifActiveRidesExist() {
        driverUser.setInactiveRequested(true);
        driverUser.setActive(true);

        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        // First call (for active rides check) returns true
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverUser.getId()), anyList()))
                .thenReturn(true);

        rideService.endRide(1L, validEndRequest);

        assertTrue(driverUser.isActive());
        assertTrue(driverUser.isInactiveRequested());
        verify(userRepository, never()).save(driverUser);
    }

    // ═══════════════════════════════════════════════════════════════
    //  WebSocket broadcast verification
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("endRide - broadcasts ride update via WebSocket")
    void endRide_broadcastsRideUpdate() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.of(vehicle));
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        RideResponse response = rideService.endRide(1L, validEndRequest);

        verify(rideSocketService).broadcastRideUpdate(eq(1L), argThat(r ->
                r.getStatus() == RideStatus.FINISHED && r.getId().equals(1L)));
    }

    @Test
    @DisplayName("endRide - notifications sent even when vehicle not found")
    void endRide_notificationsSent_evenWhenNoVehicle() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(inProgressRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDriverId(driverUser.getId())).thenReturn(Optional.empty());
        when(rideRepository.findByDriverIdAndStatusInOrderByStartTimeAsc(eq(driverUser.getId()), anyList()))
                .thenReturn(Collections.emptyList());

        rideService.endRide(1L, validEndRequest);

        verify(notificationService).sendRideFinishedNotification(any());
        verify(notificationService).notifyLinkedPassengersRideCompleted(any());
        verify(rideSocketService).broadcastRideUpdate(eq(1L), any());
    }
}
