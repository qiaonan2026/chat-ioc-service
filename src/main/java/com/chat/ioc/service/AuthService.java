package com.chat.ioc.service;

import com.chat.ioc.entity.LoginRequest;
import com.chat.ioc.entity.LoginResponse;
import com.chat.ioc.entity.User;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    boolean validateToken(String token);
    User registerUser(User user);
    User findByUsername(String username);
}