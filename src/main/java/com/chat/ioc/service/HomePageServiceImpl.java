package com.chat.ioc.service;

import com.chat.ioc.entity.HomePageInfo;
import java.time.LocalDateTime;

public class HomePageServiceImpl implements HomePageService {
    
    private int activeUsers = 0;
    private final String version = "1.0.0";
    private final String environment = System.getProperty("spring.profiles.active", "development");

    @Override
    public HomePageInfo getHomePageInfo() {
        HomePageInfo info = new HomePageInfo(
            "Chat IOC Service",
            "A robust chat application backend service with IoC container",
            version
        );
        info.setEnvironment(environment);
        info.setActiveUsers(activeUsers);
        return info;
    }

    @Override
    public HomePageInfo getDetailedHomePageInfo() {
        HomePageInfo info = getHomePageInfo();
        info.setServerTime(LocalDateTime.now());
        return info;
    }

    @Override
    public void updateActiveUsers(int count) {
        this.activeUsers = count;
    }

    @Override
    public String ping() {
        return "pong";
    }
}