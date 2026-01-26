package com.team27.lucky3.backend.service.socket;

import com.team27.lucky3.backend.dto.response.VehicleLocationResponse;
import com.team27.lucky3.backend.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for broadcasting vehicle updates via WebSocket.
 * Broadcasts to /topic/vehicles when vehicle data changes.
 */
@Service
@RequiredArgsConstructor
public class VehicleSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final VehicleService vehicleService;

    /**
     * Broadcast current vehicle locations to all subscribed clients.
     * Called automatically every 5 seconds and can be triggered manually.
     */
    @Scheduled(fixedRate = 5000) // Broadcast every 5 seconds
    public void broadcastVehicleUpdates() {
        List<VehicleLocationResponse> vehicles = vehicleService.getPublicMapVehicles();
        messagingTemplate.convertAndSend("/topic/vehicles", vehicles);
    }

    /**
     * Manually trigger a vehicle update broadcast.
     * Call this when a vehicle status changes (e.g., driver goes active/inactive, ride starts/ends).
     */
    public void notifyVehicleUpdate() {
        broadcastVehicleUpdates();
    }
}
