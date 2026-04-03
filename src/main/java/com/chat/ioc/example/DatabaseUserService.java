package com.chat.ioc.example;

public class DatabaseUserService implements UserService {
    @Override
    public User getUser(String userId) {
        // Simulate database lookup
        return new User(userId, "User-" + userId);
    }
}