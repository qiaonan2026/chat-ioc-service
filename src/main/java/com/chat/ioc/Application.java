package com.chat.ioc;

import com.chat.ioc.config.AppConfig;
import com.chat.ioc.controller.AuthController;
import com.chat.ioc.controller.HomeController;
import com.chat.ioc.entity.ApiResponse;
import com.chat.ioc.entity.HomePageInfo;
import com.chat.ioc.entity.LoginRequest;
import com.chat.ioc.entity.LoginResponse;
import com.chat.ioc.service.AuthService;
import com.chat.ioc.service.AuthServiceImpl;
import com.chat.ioc.service.HomePageService;
import com.chat.ioc.service.HomePageServiceImpl;

public class Application {
    
    public static void main(String[] args) {
        System.out.println("Starting Chat IOC Service...");
        
        // Create and configure the IOC container
        IOCContainer container = AppConfig.createContainer();
        
        // Manually get the services and create controllers with constructor injection
        HomePageService homePageService = (HomePageService) container.getBean("homePageService");
        HomeController homeController = new HomeController(homePageService);
        
        AuthService authService = (AuthService) container.getBean("authService");
        AuthController authController = new AuthController(authService);
        
        // Test the home endpoint
        System.out.println("\n=== Testing Home Endpoint ===");
        ApiResponse<HomePageInfo> homeResponse = homeController.getHome();
        System.out.println("Status Code: " + homeResponse.getCode());
        System.out.println("Message: " + homeResponse.getMessage());
        if (homeResponse.getData() != null) {
            System.out.println("Data: " + homeResponse.getData().getTitle());
            System.out.println("Description: " + homeResponse.getData().getDescription());
        }
        
        // Test the ping endpoint
        System.out.println("\n=== Testing Ping Endpoint ===");
        ApiResponse<String> pingResponse = homeController.ping();
        System.out.println("Ping Response: " + pingResponse.getData());
        
        // Test the health endpoint
        System.out.println("\n=== Testing Health Endpoint ===");
        ApiResponse<HomePageInfo> healthResponse = homeController.health();
        if (healthResponse.getData() != null) {
            System.out.println("Health Status: " + healthResponse.getData().getStatus());
            System.out.println("Version: " + healthResponse.getData().getVersion());
        }
        
        // Test the login endpoint
        System.out.println("\n=== Testing Login Endpoint ===");
        LoginRequest loginRequest = new LoginRequest("admin", "password");
        ApiResponse<LoginResponse> loginResponse = authController.login(loginRequest);
        System.out.println("Login Status Code: " + loginResponse.getCode());
        System.out.println("Login Message: " + loginResponse.getMessage());
        if (loginResponse.getData() != null) {
            System.out.println("Login Success: " + loginResponse.getData().isSuccess());
            if (loginResponse.getData().getUser() != null) {
                System.out.println("User: " + loginResponse.getData().getUser().getUsername());
            }
        }
        
        System.out.println("\nChat IOC Service started successfully!");
    }
}