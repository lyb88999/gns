package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gns.notification.domain.User;
import com.gns.notification.domain.UserMapper;
import com.gns.notification.dto.AuthResponse;
import com.gns.notification.dto.LoginRequest;
import com.gns.notification.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.gns.notification.dto.RegisterRequest;
import java.time.LocalDateTime;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_PREFIX = "gns:token:";
    private static final long EXPIRE_HOURS = 24;



    @Override
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new RuntimeException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("user"); // Default role
        user.setStatus(1); // Active
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // Save user
        userMapper.insert(user);

        // Generate token (auto-login)
        String token = UUID.randomUUID().toString();
        String key = TOKEN_PREFIX + token;
        
        // Store userId in Redis
        redisTemplate.opsForValue().set(key, user.getId().toString(), Duration.ofHours(EXPIRE_HOURS));

        return AuthResponse.builder()
                .token(token)
                .user(toUserInfo(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("Account is disabled");
        }

        String token = UUID.randomUUID().toString();
        String key = TOKEN_PREFIX + token;
        
        // Store userId in Redis
        redisTemplate.opsForValue().set(key, user.getId().toString(), Duration.ofHours(EXPIRE_HOURS));

        return AuthResponse.builder()
                .token(token)
                .user(toUserInfo(user))
                .build();
    }

    @Override
    public AuthResponse.UserInfo getCurrentUser(String token) {
        if (!StringUtils.hasText(token)) {
             throw new RuntimeException("Token missing");
        }
        
        // Remove Bearer prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String key = TOKEN_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            throw new RuntimeException("Invalid or expired token");
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userMapper.selectById(userId);
        
        if (user == null) {
             throw new RuntimeException("User not found");
        }

        return toUserInfo(user);
    }

    private AuthResponse.UserInfo toUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .teamId(user.getTeamId())
                .build();
    }
}
