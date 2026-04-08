package com.chat.ioc.service;

import com.chat.ioc.database.DatabaseManager;
import com.chat.ioc.entity.LoginRequest;
import com.chat.ioc.entity.LoginResponse;
import com.chat.ioc.entity.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServiceImpl implements AuthService {
    
    private final DatabaseManager databaseManager;
    // In-memory token storage (would use Redis in production)
    private final Map<String, String> validTokens = new ConcurrentHashMap<>();

    public AuthServiceImpl() {
        this.databaseManager = new DatabaseManager();
        // Initialize database
        DatabaseManager.initializeDatabase();
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            // Find user by username
            User user = findByUsername(username);

            if (user == null) {
                return new LoginResponse(false, null, null, "User not found");
            }

            // Validate password (in a real app, this would involve hashing)
            if (!user.getPassword().equals(password)) {
                return new LoginResponse(false, null, null, "Invalid credentials");
            }

            // Generate token
            String token = generateToken(user);

            // Return successful login response
            return new LoginResponse(true, token, user, "Login successful");
        } catch (Exception e) {
            e.printStackTrace();
            return new LoginResponse(false, null, null, "Internal server error during login: " + e.getMessage());
        }
    }

    @Override
    public boolean validateToken(String token) {
        return validTokens.containsValue(token);
    }

    @Override
    public User registerUser(User user) {
        // Save user to database
        return databaseManager.saveUser(user);
    }

    @Override
    public User findByUsername(String username) {
        return databaseManager.findByUsername(username);
    }

    private String generateToken(User user) {
        String token = UUID.randomUUID().toString();
        validTokens.put(String.valueOf(user.getId()), token);
        return token;
    }
}