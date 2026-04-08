package com.chat.ioc.service;

import com.chat.ioc.entity.LoginRequest;
import com.chat.ioc.entity.LoginResponse;
import com.chat.ioc.entity.User;
import com.chat.ioc.entity.UpdateUserRequest;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    boolean validateToken(String token);
    User registerUser(User user);
    User findByUsername(String username);
    User findByToken(String token);
    User updateCurrentUser(String token, UpdateUserRequest updateRequest);
}