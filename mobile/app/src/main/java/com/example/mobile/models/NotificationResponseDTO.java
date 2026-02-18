package com.example.mobile.models;

import java.time.LocalDateTime;

/**
 * Mirrors the backend's {@code NotificationResponse} DTO.
 * Used to fetch persisted notification history from {@code GET /api/notification}.
 */
public class NotificationResponseDTO {

    private Long id;
    private String text;
    private LocalDateTime timestamp;
    private String type;         // NotificationType enum name: RIDE_STATUS, PANIC, SUPPORT, etc.
    private Long recipientId;
    private String recipientName;
    private boolean isRead;
    private Long relatedEntityId;
    private String priority;

    public NotificationResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Long getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(Long relatedEntityId) { this.relatedEntityId = relatedEntityId; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    /**
     * Convert this backend DTO to the in-app {@link AppNotification} model.
     */
    public AppNotification toAppNotification() {
        AppNotification.Type notifType;
        switch (type != null ? type : "") {
            case "PANIC":
                notifType = AppNotification.Type.PANIC_ALERT;
                break;
            case "RIDE_FINISHED":
                notifType = AppNotification.Type.RIDE_FINISHED;
                break;
            case "RIDE_CANCELLED":
                notifType = AppNotification.Type.RIDE_CANCELLED;
                break;
            case "RIDE_CREATED":
                notifType = AppNotification.Type.RIDE_CREATED;
                break;
            case "DRIVER_ASSIGNMENT":
                notifType = AppNotification.Type.DRIVER_ASSIGNED;
                break;
            case "RIDE_STATUS":
            case "RIDE_INVITE":
                notifType = AppNotification.Type.RIDE_STATUS;
                break;
            case "STOP_COMPLETED":
                notifType = AppNotification.Type.STOP_COMPLETED;
                break;
            case "SUPPORT":
                notifType = AppNotification.Type.SUPPORT_MESSAGE;
                break;
            default:
                notifType = AppNotification.Type.GENERAL;
                break;
        }

        // Derive a title from the type
        String title;
        switch (notifType) {
            case PANIC_ALERT:      title = "\uD83D\uDEA8 PANIC ALERT"; break;
            case RIDE_FINISHED:    title = "Ride Completed"; break;
            case RIDE_CANCELLED:   title = "Ride Cancelled"; break;
            case RIDE_CREATED:     title = "Ride Created"; break;
            case DRIVER_ASSIGNED:  title = "Driver Assigned"; break;
            case RIDE_STATUS:      title = "Ride Update"; break;
            case STOP_COMPLETED:   title = "Stop Completed"; break;
            case SUPPORT_MESSAGE:  title = "Support"; break;
            default:               title = "Notification"; break;
        }

        AppNotification notif = new AppNotification(notifType, title, text);
        notif.setBackendId(id);
        notif.setRead(isRead);

        // Convert LocalDateTime to epoch millis
        if (timestamp != null) {
            notif.setTimestamp(timestamp.atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli());
        }

        // Map relatedEntityId to rideId or chatId based on type
        if (relatedEntityId != null) {
            if ("SUPPORT".equals(type)) {
                notif.setChatId(relatedEntityId);
            } else {
                notif.setRideId(relatedEntityId);
            }
        }

        return notif;
    }
}
