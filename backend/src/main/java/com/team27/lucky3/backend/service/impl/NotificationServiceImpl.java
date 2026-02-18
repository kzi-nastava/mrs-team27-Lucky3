package com.team27.lucky3.backend.service.impl;

import com.team27.lucky3.backend.dto.response.NotificationResponse;
import com.team27.lucky3.backend.entity.Notification;
import com.team27.lucky3.backend.entity.Ride;
import com.team27.lucky3.backend.entity.RideTrackingToken;
import com.team27.lucky3.backend.entity.User;
import com.team27.lucky3.backend.entity.enums.NotificationType;
import com.team27.lucky3.backend.entity.enums.UserRole;
import com.team27.lucky3.backend.exception.ResourceNotFoundException;
import com.team27.lucky3.backend.repository.NotificationRepository;
import com.team27.lucky3.backend.repository.RideTrackingTokenRepository;
import com.team27.lucky3.backend.repository.UserRepository;
import com.team27.lucky3.backend.service.EmailService;
import com.team27.lucky3.backend.service.FcmService;
import com.team27.lucky3.backend.service.NotificationService;
import com.team27.lucky3.backend.util.RideTrackingTokenUtils;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
    private final RideTrackingTokenRepository rideTrackingTokenRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final FcmService fcmService;
    private final RideTrackingTokenUtils rideTrackingTokenUtils;

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

        // 3. Push via FCM (async, non-blocking)
        pushFcm(recipient, text, type, relatedEntityId, saved.getId());

        // 4. Conditional email (async)
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

        // Push via FCM (async, non-blocking)
        pushFcm(invitedPassenger, text, NotificationType.RIDE_INVITE, ride.getId(), entity.getId());

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

            // Push via FCM (async, non-blocking)
            pushFcm(admin, text, NotificationType.PANIC, ride.getId(), entity.getId());
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
    //  STOP COMPLETED — Notify all participants
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void sendStopCompletedNotification(Ride ride, int stopIndex) {
        String stopLabel;
        if (stopIndex == -1) {
            stopLabel = "Start location";
        } else {
            stopLabel = "Stop " + (stopIndex + 1);
            if (ride.getStops() != null && stopIndex < ride.getStops().size()) {
                String address = ride.getStops().get(stopIndex).getAddress();
                if (address != null && !address.isEmpty()) {
                    stopLabel += " (" + address + ")";
                }
            }
        }

        String text = String.format("Ride #%d: %s has been completed.", ride.getId(), stopLabel);

        // Notify all passengers
        if (ride.getPassengers() != null) {
            for (User passenger : ride.getPassengers()) {
                sendNotification(passenger, text, NotificationType.STOP_COMPLETED, ride.getId());
            }
        }

        // Notify driver (if assigned)
        if (ride.getDriver() != null) {
            sendNotification(ride.getDriver(), text, NotificationType.STOP_COMPLETED, ride.getId());
        }

        log.info("Stop completed notification sent for ride #{}, stop index {}", ride.getId(), stopIndex);
    }

    @Override
    @Transactional
    public void notifyLinkedPassengersStopCompleted(Ride ride, int stopIndex) {
        List<String> invitedEmails = ride.getInvitedEmails();
        if (invitedEmails == null || invitedEmails.isEmpty()) {
            return;
        }

        String stopLabel;
        if (stopIndex == -1) {
            stopLabel = "Start location";
        } else {
            stopLabel = "Stop " + (stopIndex + 1);
            if (ride.getStops() != null && stopIndex < ride.getStops().size()) {
                String address = ride.getStops().get(stopIndex).getAddress();
                if (address != null && !address.isEmpty()) {
                    stopLabel += " (" + address + ")";
                }
            }
        }

        String text = String.format("Ride #%d: %s has been completed.", ride.getId(), stopLabel);

        for (String email : invitedEmails) {
            Optional<User> registeredUser = userRepository.findByEmail(email);
            if (registeredUser.isPresent()) {
                User user = registeredUser.get();
                sendNotification(user, text, NotificationType.STOP_COMPLETED, ride.getId());
                log.info("Sent stop completed notification to linked passenger {} for ride #{}",
                        email, ride.getId());
            }
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

        // Notify all passengers (push + FCM + email) — driver does NOT get this notification
        Set<User> passengers = ride.getPassengers();
        if (passengers != null) {
            for (User passenger : passengers) {
                Notification entity = buildAndSave(passenger, text,
                        NotificationType.RIDE_FINISHED, ride.getId(), PRIORITY_NORMAL);
                NotificationResponse dto = mapToResponse(entity);
                pushWebSocket(passenger.getId(), dto);

                // Send FCM push notification (async, non-blocking)
                pushFcm(passenger, text, NotificationType.RIDE_FINISHED, ride.getId(), entity.getId());

                // Send ride-summary email (async, skip dummy emails)
                sendRideSummaryEmail(passenger, ride);
            }
        }

        // Driver does NOT receive a ride-finished notification
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

        // Push notification to ALL registered passengers (includes the creator)
        if (ride.getPassengers() != null) {
            for (User passenger : ride.getPassengers()) {
                sendNotification(passenger, text, NotificationType.RIDE_CREATED, ride.getId());
            }
        }

        // Push notification to the driver
        if (ride.getDriver() != null) {
            String driverText = String.format(
                    "New ride request #%d assigned to you. Pickup: %s \u2192 Destination: %s. Estimated cost: %.2f RSD.",
                    ride.getId(), departure, destination, ride.getEstimatedCost()
            );
            sendNotification(ride.getDriver(), driverText, NotificationType.DRIVER_ASSIGNMENT, ride.getId());

            // Send email to the driver about the new assignment (async)
            sendRideCreatedEmailToDriver(ride);
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
        String cancellerRole = cancelledByDriver ? "driver" : "passenger";

        String reason = ride.getRejectionReason() != null && !ride.getRejectionReason().isEmpty()
                ? " Reason: " + ride.getRejectionReason()
                : "";

        String text = String.format(
                "Ride #%d has been cancelled by %s %s.%s",
                ride.getId(), cancellerRole, cancellerName, reason
        );

        // Push notification + email to ALL registered passengers except the canceller
        if (ride.getPassengers() != null) {
            for (User passenger : ride.getPassengers()) {
                if (passenger.getId().equals(cancelledBy.getId())) {
                    continue; // Skip the person who cancelled
                }
                sendNotification(passenger, text, NotificationType.RIDE_CANCELLED, ride.getId());
                // Send cancellation email to this passenger (async)
                sendCancellationEmail(passenger.getEmail(), passenger.getName(), ride, cancellerName, cancellerRole);
            }
        }

        // Push notification + email to the driver (if not the canceller)
        if (ride.getDriver() != null && !ride.getDriver().getId().equals(cancelledBy.getId())) {
            sendNotification(ride.getDriver(), text, NotificationType.RIDE_CANCELLED, ride.getId());
            // Send cancellation email to the driver (async)
            sendCancellationEmail(ride.getDriver().getEmail(), ride.getDriver().getName(), ride, cancellerName, cancellerRole);
        }

        log.info("Ride cancelled notification sent for ride #{} (cancelled by {} {})",
                ride.getId(), cancellerRole, cancellerName);
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

    @Override
    @Transactional
    public int deleteAllForUser(Long userId) {
        return notificationRepository.deleteAllForUser(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new IllegalStateException("Cannot delete another user's notification");
        }

        notificationRepository.delete(notification);
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
     * Sends an FCM push notification to the user's registered device.
     * Executes asynchronously and never throws — a failed FCM push must not
     * break the persist-first notification contract.
     */
    private void pushFcm(User recipient, String text,
                         NotificationType type, Long relatedEntityId,
                         Long notificationId) {
        if (!fcmService.isAvailable()) return;

        String fcmToken = recipient.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("No FCM token for user {} — skipping push", recipient.getId());
            return;
        }

        // Build a descriptive title based on notification type
        String title;
        switch (type) {
            case RIDE_STATUS:
                title = "Ride Update";
                break;
            case RIDE_INVITE:
                title = "Ride Invitation";
                break;
            case PANIC:
                title = "⚠ PANIC Alert";
                break;
            case DRIVER_ASSIGNMENT:
                title = "Driver Assigned";
                break;
            case RIDE_FINISHED:
                title = "Ride Completed";
                break;
            case RIDE_CREATED:
                title = "Ride Created";
                break;
            case RIDE_CANCELLED:
                title = "Ride Cancelled";
                break;
            case STOP_COMPLETED:
                title = "Stop Completed";
                break;
            case SUPPORT:
                title = "Support Message";
                break;
            default:
                title = "Lucky3 Notification";
        }

        // Extra data for deep-linking on the client
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("type", type.name());
        if (relatedEntityId != null) {
            data.put("rideId", String.valueOf(relatedEntityId));
        }
        if (notificationId != null) {
            data.put("notificationId", String.valueOf(notificationId));
        }
        data.put("userId", String.valueOf(recipient.getId()));

        // Add navigate_to hint for mobile deep-linking
        switch (type) {
            case SUPPORT:
                data.put("navigate_to", "support");
                break;
            case PANIC:
                data.put("navigate_to", "admin_panic");
                break;
            case RIDE_FINISHED:
            case RIDE_CANCELLED:
                if (relatedEntityId != null) {
                    data.put("navigate_to", "ride_history");
                }
                break;
            case RIDE_STATUS:
            case RIDE_INVITE:
            case RIDE_CREATED:
            case DRIVER_ASSIGNMENT:
            case STOP_COMPLETED:
                if (relatedEntityId != null) {
                    data.put("navigate_to", "active_ride");
                }
                break;
            default:
                break;
        }

        try {
            fcmService.sendToDevice(fcmToken, title, text, data);
        } catch (Exception e) {
            // FCM failure must never break the persist-first contract
            log.warn("Failed to push FCM notification to user {}: {}", recipient.getId(), e.getMessage());
        }
    }

    /**
     * Sends email asynchronously so WebSocket push is not delayed.
     */
    /**
     * Sends email asynchronously using CompletableFuture to avoid blocking the main thread.
     * This ensures email timeouts don't affect the API response.
     */
    protected void sendEmailAsync(String to, NotificationType type,
                                  String bodyText, Long relatedEntityId) {
        CompletableFuture.runAsync(() -> {
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
        });
    }

    /**
     * Sends an HTML-style linked-passenger invite email with a deep link.
     */
    private void sendLinkedPassengerEmail(User passenger, Ride ride) {
        if (passenger.getEmail() == null) return;

        String rideLink = frontendUrl + "/passenger/ride/" + ride.getId();
        final String email = passenger.getEmail();
        final String name = passenger.getName() != null ? passenger.getName() : "Passenger";
        final String startAddr = ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "Unknown";
        final String endAddr = ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : "Unknown";
        final double cost = ride.getEstimatedCost();

        CompletableFuture.runAsync(() -> {
            String body = String.format(
                    "Hi %s,\n\n" +
                    "You've been invited to join a ride on Lucky3!\n\n" +
                    "Route: %s → %s\n" +
                    "Estimated cost: %.2f RSD\n\n" +
                    "View and accept the ride here:\n%s\n\n" +
                    "If you did not expect this invite, you can safely ignore this email.\n\n" +
                    "— The Lucky3 Team",
                    name, startAddr, endAddr, cost, rideLink
            );

            try {
                emailService.sendSimpleMessage(email,
                        "Lucky3 — You've been invited to join a ride!", body);
            } catch (Exception e) {
                log.error("Failed to send linked-passenger email to {}: {}", email, e.getMessage());
            }
        });
    }

    /**
     * Sends a ride-summary email after a ride finishes (async).
     */
    private void sendRideSummaryEmail(User passenger, Ride ride) {
        if (passenger.getEmail() == null) return;

        final String email = passenger.getEmail();
        final String name = passenger.getName() != null ? passenger.getName() : "Passenger";
        final Long rideId = ride.getId();
        final String startAddr = ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "N/A";
        final String endAddr = ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : "N/A";
        final double distance = ride.getDistance() != null ? ride.getDistance() : 0.0;
        final double cost = ride.getTotalCost() != null ? ride.getTotalCost() : 0.0;
        final String startTime = ride.getStartTime() != null ? ride.getStartTime().format(FMT) : "N/A";
        final String endTime = ride.getEndTime() != null ? ride.getEndTime().format(FMT) : "N/A";
        final String driverName = ride.getDriver() != null ? ride.getDriver().getName() : "N/A";
        final String driverSurname = ride.getDriver() != null ? ride.getDriver().getSurname() : "";

        CompletableFuture.runAsync(() -> {
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
                    name, rideId, startAddr, endAddr, distance, cost, startTime, endTime, driverName, driverSurname
            );

            try {
                emailService.sendSimpleMessage(email, "Lucky3 — Your Ride #" + rideId + " Summary", body);
            } catch (Exception e) {
                log.error("Failed to send ride-summary email to {}: {}", email, e.getMessage());
            }
        });
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

    /**
     * Sends a cancellation email to a registered user (async).
     */
    private void sendCancellationEmail(String toEmail, String recipientName, Ride ride,
                                        String cancellerName, String cancellerRole) {
        if (toEmail == null) return;

        final String email = toEmail;
        final String name = recipientName != null ? recipientName : "User";
        final Long rideId = ride.getId();
        final String startAddr = ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "Unknown";
        final String endAddr = ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : "Unknown";
        final double cost = ride.getEstimatedCost() != null ? ride.getEstimatedCost() : 0.0;
        final String rejectionReason = ride.getRejectionReason();

        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendLinkedPassengerRideCancelledEmail(
                        email, name, rideId, startAddr, endAddr,
                        cost, cancellerName, cancellerRole, rejectionReason);
                log.info("Sent ride-cancelled email to {} for ride #{}", email, rideId);
            } catch (Exception e) {
                log.error("Failed to send ride-cancelled email to {}: {}", email, e.getMessage());
            }
        });
    }

    /**
     * Sends a ride-creation email to the assigned driver (async).
     */
    private void sendRideCreatedEmailToDriver(Ride ride) {
        if (ride.getDriver() == null) return;

        String driverEmail = ride.getDriver().getEmail();
        if (driverEmail == null) return;

        final String email = driverEmail;
        final String name = ride.getDriver().getName() != null ? ride.getDriver().getName() : "Driver";
        final Long rideId = ride.getId();
        final String startAddr = ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "Unknown";
        final String endAddr = ride.getEndLocation() != null ? ride.getEndLocation().getAddress() : "Unknown";
        final double estimatedCost = ride.getEstimatedCost() != null ? ride.getEstimatedCost() : 0.0;
        final String scheduledTime = ride.getScheduledTime() != null
                ? ride.getScheduledTime().format(FMT) : "As soon as possible";

        CompletableFuture.runAsync(() -> {
            String body = String.format(
                    "Hi %s,\n\n" +
                    "A new ride has been assigned to you on Lucky3!\n\n" +
                    "═══ Ride Details ═══\n" +
                    "Ride #%d\n" +
                    "Pickup:    %s\n" +
                    "Drop-off:  %s\n" +
                    "Estimated cost: %.2f RSD\n" +
                    "Scheduled: %s\n\n" +
                    "Please check your dashboard to accept the ride.\n\n" +
                    "— The Lucky3 Team",
                    name, rideId, startAddr, endAddr, estimatedCost, scheduledTime
            );

            try {
                emailService.sendSimpleMessage(email, "Lucky3 — New Ride #" + rideId + " Assigned", body);
                log.info("Sent ride-created email to driver {} for ride #{}", email, rideId);
            } catch (Exception e) {
                log.error("Failed to send ride-created email to driver {}: {}", email, e.getMessage());
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════
    //  LINKED PASSENGER NOTIFICATIONS
    // ════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void notifyLinkedPassengersRideCreated(Ride ride, String creatorEmail) {
        List<String> invitedEmails = ride.getInvitedEmails();
        if (invitedEmails == null || invitedEmails.isEmpty()) {
            log.debug("No linked passengers to notify for ride #{}", ride.getId());
            return;
        }

        String startAddress = ride.getStartLocation() != null 
                ? ride.getStartLocation().getAddress() : "Unknown";
        String endAddress = ride.getEndLocation() != null 
                ? ride.getEndLocation().getAddress() : "Unknown";

        for (String email : invitedEmails) {
            // Skip the ride creator - they already know about the ride
            if (creatorEmail != null && email.equalsIgnoreCase(creatorEmail)) {
                log.debug("Skipping notification for ride creator {} for ride #{}", email, ride.getId());
                continue;
            }

            // Generate tracking token for this email
            String tokenString = rideTrackingTokenUtils.generateTrackingToken(ride.getId(), email);
            
            // Persist the token
            RideTrackingToken trackingToken = new RideTrackingToken(tokenString, ride, email);
            rideTrackingTokenRepository.save(trackingToken);

            // Check if this email belongs to a registered user
            Optional<User> registeredUser = userRepository.findByEmail(email);
            String passengerName = registeredUser.map(User::getName).orElse(null);

            // Send email to ALL linked passengers (registered or not) - async
            final String emailCopy = email;
            final String passengerNameCopy = passengerName;
            final Long rideId = ride.getId();
            final String scheduledTime = ride.getScheduledTime() != null 
                    ? ride.getScheduledTime().format(FMT) : "As soon as possible";
            final String driverName = ride.getDriver() != null 
                    ? ride.getDriver().getName() + " " + ride.getDriver().getSurname() : "To be assigned";
            final double estimatedCost = ride.getEstimatedCost() != null ? ride.getEstimatedCost() : 0.0;
            
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendLinkedPassengerAddedEmail(emailCopy, passengerNameCopy, rideId,
                            startAddress, endAddress, scheduledTime, driverName, estimatedCost, tokenString);
                    log.info("Sent ride-created email to linked passenger {} for ride #{}", emailCopy, rideId);
                } catch (Exception e) {
                    log.error("Failed to send ride-created email to {}: {}", emailCopy, e.getMessage());
                }
            });

            // Send push notification ONLY to registered users
            if (registeredUser.isPresent()) {
                User user = registeredUser.get();
                String text = String.format(
                        "You've been added to ride #%d from %s to %s. Tap to track.",
                        ride.getId(), startAddress, endAddress
                );
                sendNotification(user, text, NotificationType.RIDE_INVITE, ride.getId());
                log.info("Sent push notification to registered linked passenger {} for ride #{}", 
                        email, ride.getId());
            }
        }

        log.info("Notified {} linked passengers for ride #{}", invitedEmails.size(), ride.getId());
    }

    @Override
    @Transactional
    public void notifyLinkedPassengersRideCompleted(Ride ride) {
        List<String> invitedEmails = ride.getInvitedEmails();
        if (invitedEmails == null || invitedEmails.isEmpty()) {
            log.debug("No linked passengers to notify for completed ride #{}", ride.getId());
            return;
        }

        String startAddress = ride.getStartLocation() != null 
                ? ride.getStartLocation().getAddress() : "Unknown";
        String endAddress = ride.getEndLocation() != null 
                ? ride.getEndLocation().getAddress() : "Unknown";

        for (String email : invitedEmails) {
            // Check if this email belongs to a registered user
            Optional<User> registeredUser = userRepository.findByEmail(email);
            String passengerName = registeredUser.map(User::getName).orElse(null);

            // Send email to ALL linked passengers - async
            final String emailCopy = email;
            final String passengerNameCopy = passengerName;
            final Long rideId = ride.getId();
            final double distance = ride.getDistance() != null ? ride.getDistance() : 0.0;
            final double totalCost = ride.getTotalCost() != null ? ride.getTotalCost() : 0.0;
            final String startTime = ride.getStartTime() != null ? ride.getStartTime().format(FMT) : "N/A";
            final String endTime = ride.getEndTime() != null ? ride.getEndTime().format(FMT) : "N/A";
            
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendLinkedPassengerRideCompletedEmail(emailCopy, passengerNameCopy, rideId,
                            startAddress, endAddress, distance, totalCost, startTime, endTime);
                    log.info("Sent ride-completed email to linked passenger {} for ride #{}", emailCopy, rideId);
                } catch (Exception e) {
                    log.error("Failed to send ride-completed email to {}: {}", emailCopy, e.getMessage());
                }
            });

            // Send push notification ONLY to registered users
            if (registeredUser.isPresent()) {
                User user = registeredUser.get();
                String text = String.format(
                        "Ride #%d from %s to %s has been completed! Distance: %.2f km, Cost: %.2f RSD.",
                        ride.getId(), startAddress, endAddress,
                        ride.getDistance() != null ? ride.getDistance() : 0.0,
                        ride.getTotalCost() != null ? ride.getTotalCost() : 0.0
                );
                sendNotification(user, text, NotificationType.RIDE_FINISHED, ride.getId());
            }
        }

        // Revoke all tracking tokens for this ride
        rideTrackingTokenRepository.revokeAllByRideId(ride.getId());
        log.info("Revoked tracking tokens and notified {} linked passengers for completed ride #{}", 
                invitedEmails.size(), ride.getId());
    }

    @Override
    @Transactional
    public void notifyLinkedPassengersRideStatusChange(Ride ride, String statusMessage) {
        List<String> invitedEmails = ride.getInvitedEmails();
        if (invitedEmails == null || invitedEmails.isEmpty()) {
            log.debug("No linked passengers to notify for ride #{} status change", ride.getId());
            return;
        }

        for (String email : invitedEmails) {
            Optional<User> registeredUser = userRepository.findByEmail(email);
            if (registeredUser.isPresent()) {
                User user = registeredUser.get();
                sendNotification(user, statusMessage, NotificationType.RIDE_STATUS, ride.getId());
                log.info("Sent ride status change notification to linked passenger {} for ride #{}",
                        email, ride.getId());
            }
        }
    }

    @Override
    @Transactional
    public void notifyLinkedPassengersRideCancelled(Ride ride, User cancelledBy) {
        List<String> invitedEmails = ride.getInvitedEmails();
        if (invitedEmails == null || invitedEmails.isEmpty()) {
            log.debug("No linked passengers to notify for cancelled ride #{}", ride.getId());
            return;
        }

        String cancellerName = cancelledBy.getName() + " " + cancelledBy.getSurname();
        boolean cancelledByDriver = ride.getDriver() != null 
                && ride.getDriver().getId().equals(cancelledBy.getId());
        String cancellerRole = cancelledByDriver ? "driver" : "passenger";

        String startAddress = ride.getStartLocation() != null 
                ? ride.getStartLocation().getAddress() : "Unknown";
        String endAddress = ride.getEndLocation() != null 
                ? ride.getEndLocation().getAddress() : "Unknown";

        final String rejectionReason = ride.getRejectionReason();
        final double estimatedCost = ride.getEstimatedCost();

        for (String email : invitedEmails) {
            // Check if this email belongs to a registered user
            Optional<User> registeredUser = userRepository.findByEmail(email);
            String passengerName = registeredUser.map(User::getName).orElse(null);

            // Send email to ALL linked passengers - async
            final String emailCopy = email;
            final String passengerNameCopy = passengerName;
            final Long rideId = ride.getId();
            
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendLinkedPassengerRideCancelledEmail(
                            emailCopy, passengerNameCopy, rideId, startAddress, endAddress, 
                            estimatedCost, cancellerName, cancellerRole, rejectionReason);
                    log.info("Sent ride-cancelled email to linked passenger {} for ride #{}", emailCopy, rideId);
                } catch (Exception e) {
                    log.error("Failed to send ride-cancelled email to {}: {}", emailCopy, e.getMessage());
                }
            });

            // Send push notification ONLY to registered users
            if (registeredUser.isPresent()) {
                User user = registeredUser.get();
                String reason = ride.getRejectionReason() != null && !ride.getRejectionReason().isEmpty()
                        ? " Reason: " + ride.getRejectionReason() : "";
                String text = String.format(
                        "Ride #%d from %s to %s has been cancelled by %s (%s).%s",
                        ride.getId(), startAddress, endAddress, cancellerName, cancellerRole, reason
                );
                sendNotification(user, text, NotificationType.RIDE_CANCELLED, ride.getId());
            }
        }

        // Revoke all tracking tokens for this ride
        rideTrackingTokenRepository.revokeAllByRideId(ride.getId());
        log.info("Revoked tracking tokens and notified {} linked passengers for cancelled ride #{}", 
                invitedEmails.size(), ride.getId());
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
