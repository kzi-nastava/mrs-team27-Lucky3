package com.example.mobile.models;

/**
 * Request DTO for sending a support chat message.
 * Mirrors the backend {@code SupportMessageRequest}.
 */
public class SupportMessageRequest {

    private String content;

    public SupportMessageRequest() {}

    public SupportMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
