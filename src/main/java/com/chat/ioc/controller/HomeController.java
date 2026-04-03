package com.chat.ioc.controller;

import com.chat.ioc.entity.ApiResponse;
import com.chat.ioc.entity.HomePageInfo;
import com.chat.ioc.service.HomePageService;

public class HomeController {
    
    private HomePageService homePageService;

    // Constructor injection
    public HomeController(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    public ApiResponse<HomePageInfo> getHome() {
        try {
            HomePageInfo homePageInfo = homePageService.getHomePageInfo();
            return ApiResponse.success("Welcome to Chat IOC Service", homePageInfo);
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve home page info: " + e.getMessage());
        }
    }

    public ApiResponse<HomePageInfo> getDetailedHome() {
        try {
            HomePageInfo homePageInfo = homePageService.getDetailedHomePageInfo();
            return ApiResponse.success("Detailed home page info retrieved successfully", homePageInfo);
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve detailed home page info: " + e.getMessage());
        }
    }

    public ApiResponse<String> ping() {
        try {
            String result = homePageService.ping();
            return ApiResponse.success("Ping successful", result);
        } catch (Exception e) {
            return ApiResponse.error("Ping failed: " + e.getMessage());
        }
    }

    public ApiResponse<HomePageInfo> health() {
        try {
            HomePageInfo info = homePageService.getHomePageInfo();
            info.setStatus("UP");
            return ApiResponse.success("Health check passed", info);
        } catch (Exception e) {
            return ApiResponse.error("Health check failed: " + e.getMessage());
        }
    }
}