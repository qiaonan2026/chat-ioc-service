package com.chat.ioc.entity;

import java.time.Instant;

public class ChatSession {
    private String sessionId;
    private Instant createdAt;
    private Instant updatedAt;

    public ChatSession() {}

    public ChatSession(String sessionId, Instant createdAt) {
        this.sessionId = sessionId;
        this.createdAt = createdAt;
    }

    public ChatSession(String sessionId, Instant createdAt, Instant updatedAt) {
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
