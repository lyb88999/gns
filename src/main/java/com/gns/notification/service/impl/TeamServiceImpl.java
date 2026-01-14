package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gns.notification.domain.Team;
import com.gns.notification.domain.TeamMapper;
import com.gns.notification.dto.PageResult;
import com.gns.notification.dto.TeamRequest;
import com.gns.notification.dto.TeamResponse;
import com.gns.notification.exception.AccessDeniedException;
import com.gns.notification.exception.UnauthorizedException;
import com.gns.notification.security.UserContext;
import com.gns.notification.security.UserContextHolder;
import com.gns.notification.service.TeamService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;

    public TeamServiceImpl(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    @Override
    public TeamResponse createTeam(TeamRequest request) {
        ensureAdmin();
        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        teamMapper.insert(team);
        return toResponse(team);
    }

    @Override
    public TeamResponse updateTeam(Long id, TeamRequest request) {
        ensureAdmin();
        Team team = findTeam(id);
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        teamMapper.updateById(team);
        return toResponse(team);
    }

    @Override
    public TeamResponse getTeam(Long id) {
        ensureAdminOrTeamMember(id);
        return toResponse(findTeam(id));
    }

    @Override
    public PageResult<TeamResponse> listTeams(Pageable pageable) {
        ensureAdmin();
        Page<Team> page = new Page<>(pageable.getPageNumber() + 1L, pageable.getPageSize());
        Page<Team> result = teamMapper.selectPage(page, new LambdaQueryWrapper<>());
        List<TeamResponse> responses = result.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new PageResult<>(responses, result.getTotal(), result.getPages());
    }

    @Override
    public void deleteTeam(Long id) {
        ensureAdmin();
        findTeam(id);
        teamMapper.deleteById(id);
    }

    private Team findTeam(Long id) {
        Team team = teamMapper.selectById(id);
        if (Objects.isNull(team)) {
            throw new IllegalArgumentException("Team not found: " + id);
        }
        return team;
    }

    private void ensureAdmin() {
        UserContext context = UserContextHolder.get();
        if (Objects.isNull(context)) {
            throw new UnauthorizedException("用户未登录");
        }
        if (!context.isAdmin()) {
            throw new AccessDeniedException("仅管理员可执行该操作");
        }
    }

    private void ensureAdminOrTeamMember(Long teamId) {
        UserContext context = UserContextHolder.requireUser();
        if (context.isAdmin()) {
            return;
        }
        if (context.getTeamId() != null && context.getTeamId().equals(teamId)) {
            return;
        }
        throw new AccessDeniedException("没有权限查看该团队");
    }

    private TeamResponse toResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setDescription(team.getDescription());
        response.setCreatedAt(team.getCreatedAt());
        response.setUpdatedAt(team.getUpdatedAt());
        return response;
    }
}
