package com.chat.ioc.config;

import com.chat.ioc.IOCContainer;
import com.chat.ioc.controller.HomeController;
import com.chat.ioc.service.HomePageService;
import com.chat.ioc.service.HomePageServiceImpl;

public class AppConfig {
    
    public static IOCContainer createContainer() {
        IOCContainer container = new IOCContainer();
        
        // Register services
        container.register("homePageService", HomePageServiceImpl.class);
        
        // For HomeController, we'll handle the dependency manually since constructor injection isn't fully implemented
        container.register("homeController", HomeController.class);
        
        return container;
    }
}