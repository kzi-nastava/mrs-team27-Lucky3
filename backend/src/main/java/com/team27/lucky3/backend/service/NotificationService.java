package com.team27.lucky3.backend.service;

import com.team27.lucky3.backend.dto.response.NotificationResponse;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Core notification service.
 * <p>
 * Every public method follows the persist-first contract:
 * <ol>
 *   <li>Save {@code Notification} entity to the database</li>
 *   <li>Push the DTO via WebSocket to the user's personal queue</li>
 *   <li>Optionally send an email (async) for qualifying event types</li>
 * </ol>
 */
public interface NotificationService {

    // ─── generic fan-out ───────────────────────────────────────────────

    /**
     * Persist a notification and push it via WebSocket.
     * Email is sent only when the type qualifies (RIDE_INVITE, RIDE_FINISHED).
     *
     * @return the saved NotificationResponse (includes the generated ID)
     */
    NotificationResponse sendNotification(User recipient,
                                          String text,
                                          NotificationType type,
                                          Long relatedEntityId);

    /**
     * Same as {@link #sendNotification} but allows overriding the priority
     * field (used by PANIC to set "CRITICAL").
     */
    NotificationResponse sendNotification(User recipient,
                                          String text,
                                          NotificationType type,
                                          Long relatedEntityId,
                                          String priority);

    // ─── scenario-specific helpers ─────────────────────────────────────

    /** SCENARIO A — Linked-passenger invite (Push + HTML email). */
    void sendLinkedPassengerInvite(Ride ride, User invitedPassenger);

    /** SCENARIO B — PANIC alert → all admins, priority CRITICAL. */
    void sendPanicNotification(Ride ride, User triggeredBy, String reason);

    /** SCENARIO C — Ride status change → driver + all passengers. */
    void sendRideStatusNotification(Ride ride, String statusMessage);

    /** SCENARIO C (finish) — Ride finished → push + email summary to all passengers. */
    void sendRideFinishedNotification(Ride ride);

    /** Driver-assignment notification. */
    void sendDriverAssignmentNotification(Ride ride);

    /** SUPPORT CHAT — User sent a message → notify all admins. */
    void sendSupportMessageToAdmins(User sender, Long chatId, String messagePreview);

    /** SUPPORT CHAT — Admin replied → notify the user who owns the chat. */
    void sendSupportReplyToUser(User chatOwner, Long chatId);

    /** RIDE CREATED — Notify passengers when ride is created. */
    void sendRideCreatedNotification(Ride ride);

    /** RIDE CANCELLED — Notify the other party when ride is cancelled. */
    void sendRideCancelledNotification(Ride ride, User cancelledBy);

    /** SCHEDULED RIDE REMINDER — 15 min before scheduled start. */
    void sendScheduledRideReminder(Ride ride);

    // ─── history / read-state endpoints ────────────────────────────────

    /** Paginated notification history for a user. */
    Page<NotificationResponse> getNotificationsForUser(Long userId, Pageable pageable);

    /** Paginated + filtered by type. */
    Page<NotificationResponse> getNotificationsForUser(Long userId,
                                                       NotificationType type,
                                                       Pageable pageable);

    /** Mark a single notification as read. */
    NotificationResponse markAsRead(Long notificationId, Long userId);

    /** Mark all unread notifications as read for a user. Returns count updated. */
    int markAllAsRead(Long userId);

    /** Unread count for badge display. */
    long getUnreadCount(Long userId);
}
