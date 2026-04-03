package com.chat.ioc;

import com.chat.ioc.config.AppConfig;
import com.chat.ioc.controller.HomeController;
import com.chat.ioc.entity.ApiResponse;
import com.chat.ioc.entity.HomePageInfo;
import com.chat.ioc.service.HomePageService;
import com.chat.ioc.service.HomePageServiceImpl;

public class ServiceIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("Running Service Integration Test...");
        
        // Test 1: Direct service usage
        System.out.println("\n=== Test 1: Direct Service Usage ===");
        HomePageServiceImpl service = new HomePageServiceImpl();
        HomePageInfo info = service.getHomePageInfo();
        System.out.println("Service Title: " + info.getTitle());
        System.out.println("Service Description: " + info.getDescription());
        System.out.println("Service Version: " + info.getVersion());
        
        // Test 2: Service through IOC container
        System.out.println("\n=== Test 2: Service Through IOC Container ===");
        IOCContainer container = AppConfig.createContainer();
        HomePageService serviceFromContainer = (HomePageService) container.getBean("homePageService");
        HomePageInfo infoFromContainer = serviceFromContainer.getHomePageInfo();
        System.out.println("Container Service Title: " + infoFromContainer.getTitle());
        System.out.println("Container Service Description: " + infoFromContainer.getDescription());
        System.out.println("Container Service Version: " + infoFromContainer.getVersion());
        
        // Test 3: Controller with service
        System.out.println("\n=== Test 3: Controller Integration ===");
        HomeController controller = new HomeController(serviceFromContainer);
        ApiResponse<HomePageInfo> response = controller.getHome();
        System.out.println("Response Code: " + response.getCode());
        System.out.println("Response Message: " + response.getMessage());
        if (response.getData() != null) {
            System.out.println("Response Data Title: " + response.getData().getTitle());
            System.out.println("Response Data Status: " + response.getData().getStatus());
        }
        
        // Test 4: All endpoints
        System.out.println("\n=== Test 4: All Endpoints ===");
        System.out.println("Ping: " + controller.ping().getData());
        System.out.println("Health Status: " + controller.health().getData().getStatus());
        System.out.println("Detailed Info Title: " + controller.getDetailedHome().getData().getTitle());
        
        System.out.println("\nAll integration tests passed!");
    }
}