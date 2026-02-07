package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.response.NotificationResponse;
import com.team27.lucky3.backend.entity.Notification;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.NotificationType;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.NotificationRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.EmailService;
import com.team27.lucky3.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the Notification Subsystem.
 * <p>
 * <b>Contract:</b> every notification is persisted in the database <em>before</em>
 * any real-time or email delivery attempt.  This guarantees a complete notification
 * history regardless of WebSocket connectivity or email failures.
 * <p>
 * Fan-out decision matrix:
 * <table>
 *   <tr><th>Type</th><th>WebSocket</th><th>Email</th></tr>
 *   <tr><td>RIDE_STATUS</td><td>YES</td><td>NO</td></tr>
 *   <tr><td>RIDE_INVITE</td><td>YES</td><td>YES (HTML with accept link)</td></tr>
 *   <tr><td>PANIC</td><td>YES (CRITICAL priority)</td><td>NO (admins are on-platform)</td></tr>
 *   <tr><td>SUPPORT</td><td>YES</td><td>NO</td></tr>
 *   <tr><td>DRIVER_ASSIGNMENT</td><td>YES</td><td>NO</td></tr>
 *   <tr><td>RIDE_FINISHED</td><td>YES</td><td>YES (ride summary)</td></tr>
 * </table>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    private static final String PRIORITY_NORMAL = "NORMAL";
    private static final String PRIORITY_CRITICAL = "CRITICAL";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // ════════════════════════════════════════════════════════════════════
    //  GENERIC SEND — single recipient
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public NotificationResponse sendNotification(User recipient,
                                                 String text,
                                                 NotificationType type,
                                                 Long relatedEntityId) {
        return sendNotification(recipient, text, type, relatedEntityId, PRIORITY_NORMAL);
    }

    @Override
    @Transactional
    public NotificationResponse sendNotification(User recipient,
                                                 String text,
                                                 NotificationType type,
                                                 Long relatedEntityId,
                                                 String priority) {
        // 1. Persist
        Notification entity = new Notification();
        entity.setText(text);
        entity.setTimestamp(LocalDateTime.now());
        entity.setType(type);
        entity.setRecipient(recipient);
        entity.setRead(false);
        entity.setRelatedEntityId(relatedEntityId);
        entity.setPriority(priority != null ? priority : PRIORITY_NORMAL);

        Notification saved = notificationRepository.save(entity);
        NotificationResponse dto = mapToResponse(saved);

        // 2. Push via WebSocket → /user/{id}/queue/notifications
        pushWebSocket(recipient.getId(), dto);

        // 3. Conditional email (async)
        if (type == NotificationType.RIDE_INVITE || type == NotificationType.RIDE_FINISHED) {
            sendEmailAsync(recipient.getEmail(), type, text, relatedEntityId);
        }

        log.info("Notification #{} [{}] sent to user {} (priority={})",
                saved.getId(), type, recipient.getId(), priority);

        return dto;
    }

    // ════════════════════════════════════════════════════════════════════
    //  SCENARIO A — Linked Passenger Invite
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendLinkedPassengerInvite(Ride ride, User invitedPassenger) {
        String text = String.format(
                "You've been invited to join ride #%d from %s to %s. Click to view and accept.",
                ride.getId(),
                ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "Unknown",
                ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : "Unknown"
        );

        // Persist + push WebSocket
        Notification entity = buildAndSave(invitedPassenger, text,
                NotificationType.RIDE_INVITE, ride.getId(), PRIORITY_NORMAL);
        NotificationResponse dto = mapToResponse(entity);
        pushWebSocket(invitedPassenger.getId(), dto);

        // Send HTML email with accept link
        sendLinkedPassengerEmail(invitedPassenger, ride);

        log.info("Linked-passenger invite notification sent to {} for ride #{}",
                invitedPassenger.getEmail(), ride.getId());
    }

    // ════════════════════════════════════════════════════════════════════
    //  SCENARIO B — PANIC  →  ALL Admins (priority: CRITICAL)
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendPanicNotification(Ride ride, User triggeredBy, String reason) {
        String text = String.format(
                "PANIC on ride #%d — %s %s pressed panic. Reason: %s",
                ride.getId(),
                triggeredBy.getName(),
                triggeredBy.getSurname(),
                reason != null ? reason : "No reason provided"
        );

        List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);

        for (User admin : admins) {
            Notification entity = buildAndSave(admin, text,
                    NotificationType.PANIC, ride.getId(), PRIORITY_CRITICAL);
            NotificationResponse dto = mapToResponse(entity);
            pushWebSocket(admin.getId(), dto);
        }

        log.warn("PANIC notification broadcast to {} admins for ride #{}",
                admins.size(), ride.getId());
    }

    // ════════════════════════════════════════════════════════════════════
    //  SCENARIO C — Ride Status Change  →  driver + all passengers
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendRideStatusNotification(Ride ride, String statusMessage) {
        String text = String.format("Ride #%d: %s", ride.getId(), statusMessage);

        // Notify all passengers
        if (ride.getPassengers() != null) {
            for (User passenger : ride.getPassengers()) {
                sendNotification(passenger, text, NotificationType.RIDE_STATUS, ride.getId());
            }
        }

        // Notify driver (if assigned)
        if (ride.getDriver() != null) {
            sendNotification(ride.getDriver(), text, NotificationType.RIDE_STATUS, ride.getId());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  SCENARIO C (finish) — Ride Finished → push + email to passengers
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendRideFinishedNotification(Ride ride) {
        log.info("Sending ride-finished notifications for ride #{}", ride.getId());

        String text = buildRideSummaryText(ride);

        // Notify all passengers (push + email)
        Set<User> passengers = ride.getPassengers();
        if (passengers != null) {
            for (User passenger : passengers) {
                Notification entity = buildAndSave(passenger, text,
                        NotificationType.RIDE_FINISHED, ride.getId(), PRIORITY_NORMAL);
                NotificationResponse dto = mapToResponse(entity);
                pushWebSocket(passenger.getId(), dto);

                // Send ride-summary email (async, skip dummy emails)
                sendRideSummaryEmail(passenger, ride);
            }
        }

        // Notify the driver (push only, no email)
        if (ride.getDriver() != null) {
            String driverText = String.format("Ride #%d completed. Cost: %.2f RSD, Distance: %.2f km",
                    ride.getId(), ride.getTotalCost(), ride.getDistance());
            sendNotification(ride.getDriver(), driverText, NotificationType.RIDE_FINISHED, ride.getId());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Driver-assignment notification
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendDriverAssignmentNotification(Ride ride) {
        if (ride.getDriver() == null) return;

        String departure = ride.getStartLocation() != null
                ? ride.getStartLocation().getAddress() : "Unknown";
        String destination = ride.getEndLocation() != null
                ? ride.getEndLocation().getAddress() : "Unknown";

        String text = String.format(
                "New ride request #%d assigned to you. Pickup: %s → Destination: %s. Estimated cost: %.2f RSD.",
                ride.getId(), departure, destination, ride.getEstimatedCost()
        );

        sendNotification(ride.getDriver(), text, NotificationType.DRIVER_ASSIGNMENT, ride.getId());
    }

    // ════════════════════════════════════════════════════════════════════
    //  SUPPORT CHAT — User message → Notify all Admins
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendSupportMessageToAdmins(User sender, Long chatId, String messagePreview) {
        String preview = messagePreview.length() > 50 
                ? messagePreview.substring(0, 50) + "..." 
                : messagePreview;
        
        String text = String.format(
                "New support message from %s %s: \"%s\"",
                sender.getName(),
                sender.getSurname(),
                preview
        );

        List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
        for (User admin : admins) {
            sendNotification(admin, text, NotificationType.SUPPORT, chatId);
        }

        log.info("Support message notification sent to {} admins for chat #{}", admins.size(), chatId);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SUPPORT CHAT — Admin reply → Notify the user
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendSupportReplyToUser(User chatOwner, Long chatId) {
        String text = "Support team has replied to your message. Click to view.";
        sendNotification(chatOwner, text, NotificationType.SUPPORT, chatId);
        log.info("Support reply notification sent to user {} for chat #{}", chatOwner.getEmail(), chatId);
    }

    // ════════════════════════════════════════════════════════════════════
    //  RIDE CREATED — Notify passengers
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendRideCreatedNotification(Ride ride) {
        String departure = ride.getStartLocation() != null
                ? ride.getStartLocation().getAddress() : "Unknown";
        String destination = ride.getEndLocation() != null
                ? ride.getEndLocation().getAddress() : "Unknown";

        String text = String.format(
                "Ride #%d has been created. From %s to %s. Estimated cost: %.2f RSD.",
                ride.getId(), departure, destination, ride.getEstimatedCost()
        );

        // Notify all passengers
        if (ride.getPassengers() != null) {
            for (User passenger : ride.getPassengers()) {
                sendNotification(passenger, text, NotificationType.RIDE_CREATED, ride.getId());
            }
        }

        log.info("Ride created notification sent for ride #{}", ride.getId());
    }

    // ════════════════════════════════════════════════════════════════════
    //  RIDE CANCELLED — Notify the other party
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendRideCancelledNotification(Ride ride, User cancelledBy) {
        String cancellerName = cancelledBy.getName() + " " + cancelledBy.getSurname();
        boolean cancelledByDriver = ride.getDriver() != null 
                && ride.getDriver().getId().equals(cancelledBy.getId());

        String reason = ride.getRejectionReason() != null && !ride.getRejectionReason().isEmpty()
                ? " Reason: " + ride.getRejectionReason()
                : "";

        if (cancelledByDriver) {
            // Driver cancelled → notify all passengers
            String text = String.format(
                    "Ride #%d has been cancelled by driver %s.%s",
                    ride.getId(), cancellerName, reason
            );

            if (ride.getPassengers() != null) {
                for (User passenger : ride.getPassengers()) {
                    sendNotification(passenger, text, NotificationType.RIDE_CANCELLED, ride.getId());
                }
            }
            log.info("Ride cancelled notification sent to passengers for ride #{}", ride.getId());
        } else {
            // Passenger cancelled → notify driver
            if (ride.getDriver() != null) {
                String text = String.format(
                        "Ride #%d has been cancelled by passenger %s.%s",
                        ride.getId(), cancellerName, reason
                );
                sendNotification(ride.getDriver(), text, NotificationType.RIDE_CANCELLED, ride.getId());
                log.info("Ride cancelled notification sent to driver for ride #{}", ride.getId());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  SCHEDULED RIDE REMINDER — 15 min before start
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendScheduledRideReminder(Ride ride) {
        String departure = ride.getStartLocation() != null
                ? ride.getStartLocation().getAddress() : "Unknown";
        String destination = ride.getEndLocation() != null
                ? ride.getEndLocation().getAddress() : "Unknown";

        String text = String.format(
                "Reminder: Your ride #%d from %s to %s starts in 15 minutes!",
                ride.getId(), departure, destination
        );

        // Notify all passengers
        if (ride.getPassengers() != null) {
            for (User passenger : ride.getPassengers()) {
                sendNotification(passenger, text, NotificationType.RIDE_SCHEDULED_REMINDER, ride.getId());
            }
        }

        // Notify driver
        if (ride.getDriver() != null) {
            String driverText = String.format(
                    "Reminder: Ride #%d pickup at %s starts in 15 minutes!",
                    ride.getId(), departure
            );
            sendNotification(ride.getDriver(), driverText, NotificationType.RIDE_SCHEDULED_REMINDER, ride.getId());
        }

        log.info("Scheduled ride reminder sent for ride #{}", ride.getId());
    }

    // ════════════════════════════════════════════════════════════════════
    //  HISTORY / READ-STATE
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRepository
                .findByRecipientIdOrderByTimestampDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForUser(Long userId,
                                                              NotificationType type,
                                                              Pageable pageable) {
        return notificationRepository
                .findByRecipientIdAndTypeOrderByTimestampDesc(userId, type, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new IllegalStateException("Cannot mark another user's notification as read");
        }

        notification.setRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllReadForUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    // ════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════

    private Notification buildAndSave(User recipient, String text,
                                      NotificationType type, Long relatedEntityId,
                                      String priority) {
        Notification n = new Notification();
        n.setText(text);
        n.setTimestamp(LocalDateTime.now());
        n.setType(type);
        n.setRecipient(recipient);
        n.setRead(false);
        n.setRelatedEntityId(relatedEntityId);
        n.setPriority(priority);
        return notificationRepository.save(n);
    }

    private void pushWebSocket(Long userId, NotificationResponse dto) {
        String dest = "/user/" + userId + "/queue/notifications";
        try {
            messagingTemplate.convertAndSend(dest, dto);
            log.debug("WS push to {} → {}", dest, dto.getType());
        } catch (Exception e) {
            // WebSocket failure must never break the persist-first contract
            log.warn("Failed to push WS notification to {}: {}", dest, e.getMessage());
        }
    }

    /**
     * Sends email asynchronously so WebSocket push is not delayed.
     */
    @Async
    protected void sendEmailAsync(String to, NotificationType type,
                                  String bodyText, Long relatedEntityId) {
        try {
            String subject;
            switch (type) {
                case RIDE_INVITE:
                    subject = "Lucky3 — You've been invited to a ride!";
                    break;
                case RIDE_FINISHED:
                    subject = "Lucky3 — Your ride summary";
                    break;
                default:
                    subject = "Lucky3 Notification";
            }
            emailService.sendSimpleMessage(to, subject, bodyText);
        } catch (Exception e) {
            log.error("Async email to {} failed: {}", to, e.getMessage());
        }
    }

    /**
     * Sends an HTML-style linked-passenger invite email with a deep link.
     */
    private void sendLinkedPassengerEmail(User passenger, Ride ride) {
        if (passenger.getEmail() == null || passenger.getEmail().endsWith("@example.com")) return;

        String rideLink = frontendUrl + "/passenger/ride/" + ride.getId();

        String body = String.format(
                "Hi %s,\n\n" +
                "You've been invited to join a ride on Lucky3!\n\n" +
                "Route: %s → %s\n" +
                "Estimated cost: %.2f RSD\n\n" +
                "View and accept the ride here:\n%s\n\n" +
                "If you did not expect this invite, you can safely ignore this email.\n\n" +
                "— The Lucky3 Team",
                passenger.getName() != null ? passenger.getName() : "Passenger",
                ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "Unknown",
                ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : "Unknown",
                ride.getEstimatedCost(),
                rideLink
        );

        try {
            emailService.sendSimpleMessage(passenger.getEmail(),
                    "Lucky3 — You've been invited to join a ride!", body);
        } catch (Exception e) {
            log.error("Failed to send linked-passenger email to {}: {}",
                    passenger.getEmail(), e.getMessage());
        }
    }

    /**
     * Sends a ride-summary email after a ride finishes.
     */
    private void sendRideSummaryEmail(User passenger, Ride ride) {
        if (passenger.getEmail() == null || passenger.getEmail().endsWith("@example.com")) return;

        String body = String.format(
                "Hi %s,\n\n" +
                "Your ride #%d has been completed!\n\n" +
                "═══ Ride Summary ═══\n" +
                "From:     %s\n" +
                "To:       %s\n" +
                "Distance: %.2f km\n" +
                "Cost:     %.2f RSD\n" +
                "Start:    %s\n" +
                "End:      %s\n" +
                "Driver:   %s %s\n\n" +
                "Thank you for riding with Lucky3!\n\n" +
                "— The Lucky3 Team",
                passenger.getName() != null ? passenger.getName() : "Passenger",
                ride.getId(),
                ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "N/A",
                ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : "N/A",
                ride.getDistance() != null ? ride.getDistance() : 0.0,
                ride.getTotalCost() != null ? ride.getTotalCost() : 0.0,
                ride.getStartTime() != null ? ride.getStartTime().format(FMT) : "N/A",
                ride.getEndTime() != null ? ride.getEndTime().format(FMT) : "N/A",
                ride.getDriver() != null ? ride.getDriver().getName() : "N/A",
                ride.getDriver() != null ? ride.getDriver().getSurname() : ""
        );

        try {
            emailService.sendSimpleMessage(passenger.getEmail(),
                    "Lucky3 — Your Ride #" + ride.getId() + " Summary", body);
        } catch (Exception e) {
            log.error("Failed to send ride-summary email to {}: {}",
                    passenger.getEmail(), e.getMessage());
        }
    }

    private String buildRideSummaryText(Ride ride) {
        return String.format(
                "Your ride #%d is complete! From %s to %s. Distance: %.2f km, Cost: %.2f RSD.",
                ride.getId(),
                ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "Unknown",
                ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : "Unknown",
                ride.getDistance() != null ? ride.getDistance() : 0.0,
                ride.getTotalCost() != null ? ride.getTotalCost() : 0.0
        );
    }

    // ─── entity  →  DTO mapper ─────────────────────────────────────────

    private NotificationResponse mapToResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setText(n.getText());
        r.setTimestamp(n.getTimestamp());
        r.setType(n.getType());
        r.setRecipientId(n.getRecipient().getId());
        r.setRecipientName(n.getRecipient().getName() + " " + n.getRecipient().getSurname());
        r.setRead(n.isRead());
        r.setRelatedEntityId(n.getRelatedEntityId());
        r.setPriority(n.getPriority());
        return r;
    }
}
