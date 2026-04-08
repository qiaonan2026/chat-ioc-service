package com.chat.ioc.entity;

public class LoginResponse {
    private String token;
    private User user;
    private boolean success;
    private String message;

    public LoginResponse() {}

    public LoginResponse(boolean success, String token, User user, String message) {
        this.success = success;
        this.token = token;
        this.user = user;
        this.message = message;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}