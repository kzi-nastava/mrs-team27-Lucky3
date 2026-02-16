package com.example.mobile.models;

import java.io.Serializable;

/**
 * Represents an in-app notification stored in the notification center.
 * Each notification has a type, display info, read state, and optional
 * deep-link data (e.g. rideId, chatId) for tap-to-navigate.
 */
public class AppNotification implements Serializable {

    public enum Type {
        RIDE_STATUS,        // Ride accepted, started, finished, cancelled
        RIDE_INVITE,        // Invited to join a ride
        PANIC_ALERT,        // Panic button pressed (admin)
        SUPPORT_MESSAGE,    // New support chat message
        GENERAL             // Fallback
    }

    private long id;
    private Type type;
    private String title;
    private String body;
    private long timestamp;
    private boolean read;

    // Deep-link data (nullable)
    private Long rideId;
    private Long chatId;

    public AppNotification() {
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public AppNotification(Type type, String title, String body) {
        this();
        this.type = type;
        this.title = title;
        this.body = body;
    }

    // ---- Getters / Setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }
}
