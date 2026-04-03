package com.chat.ioc.service;

import com.chat.ioc.entity.HomePageInfo;

public interface HomePageService {
    HomePageInfo getHomePageInfo();
    HomePageInfo getDetailedHomePageInfo();
    void updateActiveUsers(int count);
    String ping();
}