package com.team27.lucky3.backend.service.socket;

import com.team27.lucky3.backend.dto.response.RideResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for broadcasting ride status updates via WebSocket.
 * Broadcasts to /topic/ride/{rideId} when ride status changes.
 */
@Service
@Slf4j
public class RideSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public RideSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast a ride status update to all subscribed clients.
     * Clients should subscribe to /topic/ride/{rideId} to receive updates.
     * 
     * @param rideId The ID of the ride
     * @param rideResponse The updated ride data
     */
    public void broadcastRideUpdate(Long rideId, RideResponse rideResponse) {
        String destination = "/topic/ride/" + rideId;
        messagingTemplate.convertAndSend(destination, rideResponse);
        log.info("Broadcast ride update to {} - status: {}", destination, rideResponse.getStatus());
    }

    /**
     * Broadcast a simple status update for a ride.
     * Useful when only the status changes without full ride data.
     * 
     * @param rideId The ID of the ride
     * @param status The new status
     */
    public void broadcastRideStatusChange(Long rideId, String status) {
        String destination = "/topic/ride/" + rideId;
        messagingTemplate.convertAndSend(destination, new RideStatusUpdate(rideId, status));
        log.info("Broadcast status change to {} - new status: {}", destination, status);
    }

    /**
     * Simple DTO for status-only updates
     */
    public record RideStatusUpdate(Long rideId, String status) {}
}
