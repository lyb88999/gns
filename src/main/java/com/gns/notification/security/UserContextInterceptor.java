package com.gns.notification.security;

import com.gns.notification.domain.User;
import com.gns.notification.domain.UserMapper;
import com.gns.notification.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.gns.notification.domain.ApiTokenMapper apiTokenMapper;

    public UserContextInterceptor(UserMapper userMapper, org.springframework.data.redis.core.StringRedisTemplate redisTemplate, com.gns.notification.domain.ApiTokenMapper apiTokenMapper) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.apiTokenMapper = apiTokenMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = null;
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // 1. Check Redis (Session Token)
            String key = "gns:token:" + token;
            String userIdStr = redisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(userIdStr)) {
                userId = Long.parseLong(userIdStr);
            } else {
                // 2. Check DB (API Token)
                String hashedToken = hashToken(token);
                com.gns.notification.domain.ApiToken apiToken = apiTokenMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.gns.notification.domain.ApiToken>()
                        .eq(com.gns.notification.domain.ApiToken::getToken, hashedToken)
                );
                
                if (apiToken != null) {
                    if (apiToken.getExpiresAt() != null && apiToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                         throw new UnauthorizedException("Token 已过期");
                    }
                    userId = apiToken.getUserId();
                }
            }
        }

        if (userId == null) {
             throw new UnauthorizedException("未认证: 请提供有效的 Token");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("用户不存在或已被禁用");
        }
        UserContextHolder.set(new UserContext(user.getId(), user.getTeamId(), user.getRole()));
        return true;
    }

    private String hashToken(String rawToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashed);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
