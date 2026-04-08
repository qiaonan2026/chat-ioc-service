package com.chat.ioc.controller;

import com.chat.ioc.entity.ApiResponse;
import com.chat.ioc.entity.LoginRequest;
import com.chat.ioc.entity.LoginResponse;
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
}