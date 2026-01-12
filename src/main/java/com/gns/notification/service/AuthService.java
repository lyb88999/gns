package com.gns.notification.service;

import com.gns.notification.dto.AuthResponse;
import com.gns.notification.dto.LoginRequest;

import com.gns.notification.dto.RegisterRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse.UserInfo getCurrentUser(String token);
}
