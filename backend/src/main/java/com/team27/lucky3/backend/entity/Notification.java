package com.team27.lucky3.backend.entity;

import com.team27.lucky3.backend.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persisted notification entity.  Every notification is saved to the database
 * <b>before</b> being pushed via WebSocket or email so users can always review
 * their full notification history.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notification_recipient_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notification_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable notification body (may contain simple HTML for emails). */
    @Column(nullable = false, length = 1024)
    private String text;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** Classification used by the frontend to choose icon / sound / routing. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** The user who should receive this notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /** Has the recipient opened / acknowledged the notification? */
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    /**
     * Optional foreign-key to the related domain object (Ride, SupportChat, etc.).
     * The frontend uses this together with {@link #type} to deep-link the user
     * to the correct screen.
     */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /**
     * Priority hint consumed by mobile / web clients.
     * Normal notifications use {@code "NORMAL"};
     * PANIC notifications use {@code "CRITICAL"} so the frontend can trigger an alarm sound.
     */
    @Column(nullable = false, length = 32)
    private String priority = "NORMAL";

    // ───────── convenience factory ─────────

    @PrePersist
    private void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
