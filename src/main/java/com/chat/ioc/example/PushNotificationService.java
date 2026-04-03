package com.chat.ioc.example;

public class PushNotificationService implements NotificationService {
    @Override
    public void notifyUser(String userId, String message) {
        System.out.println("Push notification to user " + userId + ": " + message);
    }
}