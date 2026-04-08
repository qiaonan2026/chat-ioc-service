package com.chat.ioc.config;

import com.chat.ioc.IOCContainer;
import com.chat.ioc.controller.AuthController;
import com.chat.ioc.controller.HomeController;
import com.chat.ioc.service.AuthService;
import com.chat.ioc.service.AuthServiceImpl;
import com.chat.ioc.service.HomePageService;
import com.chat.ioc.service.HomePageServiceImpl;

public class AppConfig {
    
    public static IOCContainer createContainer() {
        IOCContainer container = new IOCContainer();
        
        // Register services
        container.register("homePageService", HomePageServiceImpl.class);
        container.register("authService", AuthServiceImpl.class);
        
        // Register controllers
        container.register("homeController", HomeController.class);
        container.register("authController", AuthController.class);
        
        return container;
    }
}