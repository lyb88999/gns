package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gns.notification.domain.TeamMapper;
import com.gns.notification.domain.User;
import com.gns.notification.domain.UserMapper;
import com.gns.notification.dto.PageResult;
import com.gns.notification.dto.UserRequest;
import com.gns.notification.dto.UserResponse;
import com.gns.notification.exception.AccessDeniedException;
import com.gns.notification.exception.UnauthorizedException;
import com.gns.notification.security.UserContext;
import com.gns.notification.security.UserContextHolder;
import com.gns.notification.service.UserService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, TeamMapper teamMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.teamMapper = teamMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(UserRequest request) {
        ensureAdmin();
        validateTeam(request.getTeamId());
        User user = new User();
        applyRequest(user, request, true);
        userMapper.insert(user);
        return toResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest request) {
        ensureAdmin();
        User user = findUser(id);
        validateTeam(request.getTeamId());
        applyRequest(user, request, false);
        userMapper.updateById(user);
        return toResponse(user);
    }

    @Override
    public UserResponse getUser(Long id) {
        ensureAdminOrSelf(id);
        return toResponse(findUser(id));
    }

    @Override
    public PageResult<UserResponse> listUsers(Pageable pageable) {
        ensureAdmin();
        Page<User> page = new Page<>(pageable.getPageNumber() + 1L, pageable.getPageSize());
        Page<User> result = userMapper.selectPage(page, new LambdaQueryWrapper<>());
        List<UserResponse> responses = result.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new PageResult<>(responses, result.getTotal(), result.getPages());
    }

    @Override
    public void deleteUser(Long id) {
        ensureAdmin();
        findUser(id);
        userMapper.deleteById(id);
    }

    private User findUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        return user;
    }

    private void ensureAdmin() {
        UserContext context = UserContextHolder.get();
        if (context == null) {
            throw new UnauthorizedException("用户未登录");
        }
        if (!context.isAdmin()) {
            throw new AccessDeniedException("仅管理员可操作用户");
        }
    }

    private void ensureAdminOrSelf(Long userId) {
        UserContext context = UserContextHolder.requireUser();
        if (context.isAdmin() || Objects.equals(context.getUserId(), userId)) {
            return;
        }
        throw new AccessDeniedException("没有权限查看该用户");
    }

    private void validateTeam(Long teamId) {
        if (teamId == null) {
            return;
        }
        if (teamMapper.selectById(teamId) == null) {
            throw new IllegalArgumentException("Team not found: " + teamId);
        }
    }

    private void applyRequest(User user, UserRequest request, boolean isCreate) {
        user.setUsername(request.getUsername());
        if (isCreate && !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("Password is required for new user");
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setTeamId(request.getTeamId());
        user.setStatus(request.getStatus());
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setTeamId(user.getTeamId());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
