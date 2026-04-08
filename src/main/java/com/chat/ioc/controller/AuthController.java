package com.chat.ioc.controller;

import com.chat.ioc.entity.ApiResponse;
import com.chat.ioc.entity.LoginRequest;
import com.chat.ioc.entity.LoginResponse;
import com.chat.ioc.entity.RegisterRequest;
import com.chat.ioc.entity.User;
import com.chat.ioc.service.AuthService;

public class AuthController {
    
    private AuthService authService;

    // Constructor injection
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public ApiResponse<LoginResponse> login(LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);
            
            if (loginResponse.isSuccess()) {
                return ApiResponse.success(loginResponse.getMessage(), loginResponse);
            } else {
                return ApiResponse.error(loginResponse.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Login failed due to server error: " + e.getMessage());
        }
    }

    public ApiResponse<String> logout(String token) {
        try {
            // In a real implementation, we would invalidate the token
            // For now, we just return a success response
            return ApiResponse.success("Logout successful", "Logged out successfully");
        } catch (Exception e) {
            return ApiResponse.error("Logout failed: " + e.getMessage());
        }
    }

    public ApiResponse<User> register(RegisterRequest registerRequest) {
        try {
            // Create a new user from the registration request
            User newUser = new User();
            newUser.setUsername(registerRequest.getUsername());
            newUser.setPassword(registerRequest.getPassword()); // In a real app, this should be hashed
            newUser.setEmail(registerRequest.getEmail());
            newUser.setNickname(registerRequest.getNickname());
            newUser.setIsActive(true);

            // Attempt to register the user
            User registeredUser = authService.registerUser(newUser);

            if (registeredUser != null) {
                return ApiResponse.success("User registered successfully", registeredUser);
            } else {
                return ApiResponse.error("Registration failed: username may already exist");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Registration failed due to server error: " + e.getMessage());
        }
    }
}