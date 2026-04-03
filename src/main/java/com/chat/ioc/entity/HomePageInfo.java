package com.chat.ioc.entity;

import java.time.LocalDateTime;

public class HomePageInfo {
    private String title;
    private String description;
    private String version;
    private LocalDateTime serverTime;
    private String environment;
    private int activeUsers;
    private String status;

    public HomePageInfo() {
        this.serverTime = LocalDateTime.now();
    }

    // Constructor with common fields
    public HomePageInfo(String title, String description, String version) {
        this.title = title;
        this.description = description;
        this.version = version;
        this.serverTime = LocalDateTime.now();
        this.status = "UP";
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}