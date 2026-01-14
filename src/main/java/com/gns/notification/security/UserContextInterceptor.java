package com.gns.notification.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gns.notification.domain.ApiToken;
import com.gns.notification.domain.User;
import com.gns.notification.domain.UserMapper;
import com.gns.notification.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.gns.notification.domain.ApiTokenMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final ApiTokenMapper apiTokenMapper;

    public UserContextInterceptor(UserMapper userMapper, StringRedisTemplate redisTemplate, ApiTokenMapper apiTokenMapper) {
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
                ApiToken apiToken = apiTokenMapper.selectOne(
                        new LambdaQueryWrapper<ApiToken>()
                                .eq(ApiToken::getToken, hashedToken)
                );

                if (Objects.nonNull(apiToken)) {
                    if (Objects.nonNull(apiToken.getExpiresAt()) && apiToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                        throw new UnauthorizedException("Token 已过期");
                    }
                    userId = apiToken.getUserId();
                }
            }
        }

        if (Objects.isNull(userId)) {
            throw new UnauthorizedException("未认证: 请提供有效的 Token");
        }

        User user = userMapper.selectById(userId);
        if (Objects.isNull(user) || Objects.isNull(user.getStatus()) || user.getStatus() != 1) {
            throw new UnauthorizedException("用户不存在或已被禁用");
        }
        UserContextHolder.set(new UserContext(user.getId(), user.getTeamId(), user.getRole()));
        return true;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
