package com.example.mobile.models;

/**
 * Mirrors the backend {@code SupportMessageResponse} DTO.
 * Received via WebSocket on support chat topics.
 */
public class SupportMessageResponse {

    private Long id;
    private Long chatId;
    private Long senderId;
    private String senderName;
    private String content;
    private String timestamp;
    private boolean fromAdmin;

    public SupportMessageResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isFromAdmin() { return fromAdmin; }
    public void setFromAdmin(boolean fromAdmin) { this.fromAdmin = fromAdmin; }
}
