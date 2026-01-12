package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gns.notification.domain.ApiToken;
import com.gns.notification.domain.ApiTokenMapper;
import com.gns.notification.dto.ApiTokenRequest;
import com.gns.notification.dto.ApiTokenResponse;
import com.gns.notification.dto.PageResult;
import com.gns.notification.exception.AccessDeniedException;
import com.gns.notification.exception.UnauthorizedException;
import com.gns.notification.security.UserContext;
import com.gns.notification.security.UserContextHolder;
import com.gns.notification.service.ApiTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiTokenServiceImpl implements ApiTokenService {

    private final ApiTokenMapper apiTokenMapper;

    public ApiTokenServiceImpl(ApiTokenMapper apiTokenMapper) {
        this.apiTokenMapper = apiTokenMapper;
    }

    @Override
    public ApiTokenResponse createToken(ApiTokenRequest request) {
        UserContext context = UserContextHolder.get();
        if (context == null) {
            throw new UnauthorizedException("用户未登录");
        }
        String rawToken = generateRawToken();
        ApiToken token = new ApiToken();
        token.setUserId(context.getUserId());
        token.setName(request.getName());
        token.setScopes(request.getScopes());
        token.setExpiresAt(request.getExpiresAt());
        token.setToken(hashToken(rawToken));
        apiTokenMapper.insert(token);
        ApiTokenResponse response = toResponse(token);
        response.setToken(rawToken);
        return response;
    }

    @Override
    public PageResult<ApiTokenResponse> listTokens(Long userId, Pageable pageable) {
        UserContext context = UserContextHolder.requireUser();
        LambdaQueryWrapper<ApiToken> wrapper = new LambdaQueryWrapper<>();
        if (context.isAdmin()) {
            if (userId != null) {
                wrapper.eq(ApiToken::getUserId, userId);
            }
        } else {
            wrapper.eq(ApiToken::getUserId, context.getUserId());
        }
        Page<ApiToken> page = new Page<>(pageable.getPageNumber() + 1L, pageable.getPageSize());
        Page<ApiToken> result = apiTokenMapper.selectPage(page, wrapper);
        List<ApiTokenResponse> responses = result.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new PageResult<>(responses, result.getTotal(), result.getPages());
    }

    @Override
    public ApiTokenResponse getToken(Long id) {
        ApiToken token = findToken(id);
        ensureOwnerOrAdmin(token);
        return toResponse(token);
    }

    @Override
    public void deleteToken(Long id) {
        ApiToken token = findToken(id);
        ensureOwnerOrAdmin(token);
        apiTokenMapper.deleteById(id);
    }

    private ApiToken findToken(Long id) {
        ApiToken token = apiTokenMapper.selectById(id);
        if (token == null) {
            throw new IllegalArgumentException("Token not found: " + id);
        }
        return token;
    }

    private void ensureOwnerOrAdmin(ApiToken token) {
        UserContext context = UserContextHolder.requireUser();
        if (context.isAdmin() || token.getUserId().equals(context.getUserId())) {
            return;
        }
        throw new AccessDeniedException("没有权限操作该 Token");
    }

    private String generateRawToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    private ApiTokenResponse toResponse(ApiToken token) {
        ApiTokenResponse response = new ApiTokenResponse();
        response.setId(token.getId());
        response.setUserId(token.getUserId());
        response.setName(token.getName());
        response.setScopes(token.getScopes());
        response.setLastUsedAt(token.getLastUsedAt());
        response.setExpiresAt(token.getExpiresAt());
        response.setCreatedAt(token.getCreatedAt());
        return response;
    }
}
