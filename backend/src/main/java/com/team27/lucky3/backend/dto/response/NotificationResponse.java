package com.team27.lucky3.backend.dto.response;

import com.team27.lucky3.backend.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO sent both as a REST response (history list) and as the WebSocket payload
 * pushed to <code>/user/{id}/queue/notifications</code>.
 *
 * <p><b>PANIC example payload (WebSocket):</b></p>
 * <pre>{@code
 * {
 *   "id": 42,
 *   "text": "PANIC on ride #108 — Reason: Aggressive driver",
 *   "timestamp": "2026-02-06T14:23:00",
 *   "type": "PANIC",
 *   "recipientId": 1,
 *   "recipientName": "Admin Admin",
 *   "isRead": false,
 *   "relatedEntityId": 108,
 *   "priority": "CRITICAL"
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String text;
    private LocalDateTime timestamp;
    private NotificationType type;
    private Long recipientId;
    private String recipientName;
    private boolean isRead;
    private Long relatedEntityId;

    /**
     * {@code "NORMAL"} for regular notifications,
     * {@code "CRITICAL"} for PANIC alerts.
     * <p>Frontend must play a siren/alarm sound when priority is CRITICAL.</p>
     */
    private String priority;
}
