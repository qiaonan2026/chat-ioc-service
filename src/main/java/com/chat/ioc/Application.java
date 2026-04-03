package com.chat.ioc;

import com.chat.ioc.config.AppConfig;
import com.chat.ioc.controller.HomeController;
import com.chat.ioc.entity.ApiResponse;
import com.chat.ioc.entity.HomePageInfo;
import com.chat.ioc.service.HomePageService;
import com.chat.ioc.service.HomePageServiceImpl;

public class Application {
    
    public static void main(String[] args) {
        System.out.println("Starting Chat IOC Service...");
        
        // Create and configure the IOC container
        IOCContainer container = AppConfig.createContainer();
        
        // Manually get the service and create controller with constructor injection
        HomePageService homePageService = (HomePageService) container.getBean("homePageService");
        HomeController homeController = new HomeController(homePageService);
        
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
        
        System.out.println("\nChat IOC Service started successfully!");
    }
}