package com.team27.lucky3.backend.service.socket;

import com.team27.lucky3.backend.dto.response.PanicResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for broadcasting panic alerts via WebSocket.
 * Sends real-time notifications to all subscribed administrators when a panic event occurs.
 *
 * Topics:
 * - /topic/panic - New panic alert broadcast (admins subscribe to this)
 */
@Service
@Slf4j
public class PanicSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public PanicSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast a new panic alert to all admin subscribers.
     * Called when a driver or passenger presses the panic button.
     *
     * @param panicResponse The panic event details to broadcast
     */
    public void broadcastPanicAlert(PanicResponse panicResponse) {
        String destination = "/topic/panic";
        messagingTemplate.convertAndSend(destination, panicResponse);
        log.info("Broadcasted panic alert #{} for ride #{} to {}",
                panicResponse.getId(),
                panicResponse.getRide() != null ? panicResponse.getRide().getId() : "unknown",
                destination);
    }
}
