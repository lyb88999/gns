package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import com.gns.notification.domain.User;
import com.gns.notification.domain.UserMapper;
import com.gns.notification.dto.NotificationTaskRequest;
import com.gns.notification.dto.NotificationTaskResponse;
import com.gns.notification.dto.PageResult;
import com.gns.notification.exception.AccessDeniedException;
import com.gns.notification.exception.UnauthorizedException;
import com.gns.notification.security.UserContext;
import com.gns.notification.security.UserContextHolder;
import com.gns.notification.service.NotificationTaskService;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gns.notification.service.scheduler.TaskSchedulerEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class NotificationTaskServiceImpl implements NotificationTaskService {

    private final NotificationTaskMapper mapper;
    private final UserMapper userMapper;
    private final TaskSchedulerEngine schedulerEngine;

    public NotificationTaskServiceImpl(NotificationTaskMapper mapper, 
                                       UserMapper userMapper,
                                       TaskSchedulerEngine schedulerEngine) {
        this.mapper = mapper;
        this.userMapper = userMapper;
        this.schedulerEngine = schedulerEngine;
    }

    @Override
    public NotificationTaskResponse createTask(NotificationTaskRequest request) {
        UserContext context = requireUser();
        NotificationTask entity = new NotificationTask();
        applyRequest(entity, request);
        if (!StringUtils.hasText(entity.getTaskId())) {
            entity.setTaskId(UUID.randomUUID().toString());
        }
        entity.setUserId(context.getUserId());
        entity.setTeamId(context.getTeamId());
        entity.setStatus(Boolean.TRUE);
        mapper.insert(entity);
        
        // Schedule if cron
        if ("cron".equals(entity.getTriggerType())) {
             schedulerEngine.schedule(entity);
        }
        
        return toResponse(entity);
    }

    @Override
    public NotificationTaskResponse updateTask(String taskId, NotificationTaskRequest request) {
        NotificationTask entity = fetchByTaskId(taskId);
        ensureAccess(entity);
        applyRequest(entity, request);
        mapper.updateById(entity);

        // Update schedule
        if ("cron".equals(entity.getTriggerType())) {
            schedulerEngine.schedule(entity);
        } else {
            schedulerEngine.remove(entity);
        }

        return toResponse(entity);
    }

    @Override
    public NotificationTaskResponse getTask(String taskId) {
        NotificationTask entity = fetchByTaskId(taskId);
        ensureAccess(entity);
        return toResponse(entity);
    }

    @Override
    public PageResult<NotificationTaskResponse> listTasks(Pageable pageable) {
        Page<NotificationTask> page = new Page<>(pageable.getPageNumber() + 1L, pageable.getPageSize());
        Page<NotificationTask> result = mapper.selectPage(page, scopedQuery());
        List<NotificationTaskResponse> responses = result.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new PageResult<>(responses, result.getTotal(), result.getPages());
    }

    @Override
    public void deleteTask(String taskId) {
        NotificationTask entity = fetchByTaskId(taskId);
        ensureAccess(entity);
        
        schedulerEngine.remove(entity);
        
        mapper.deleteById(entity.getId());
    }

    @Override
    public void markStreamEntry(String taskId, String streamEntryId) {
        NotificationTask entity = fetchByTaskId(taskId);
        ensureAccess(entity);
        entity.setStreamEntryId(streamEntryId);
        mapper.updateById(entity);
    }

    private NotificationTask fetchByTaskId(String taskId) {
        LambdaQueryWrapper<NotificationTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTask::getTaskId, taskId);
        return Optional.ofNullable(mapper.selectOne(wrapper))
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    private void applyRequest(NotificationTask entity, NotificationTaskRequest request) {
        entity.setTaskId(request.getTaskId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setTriggerType(request.getTriggerType());
        entity.setCronExpression(request.getCronExpression());
        entity.setChannels(request.getChannels());
        entity.setMessageTemplate(request.getMessageTemplate());
        entity.setCustomData(request.getCustomData());
        entity.setPriority(request.getPriority());
        entity.setRateLimitEnabled(request.getRateLimitEnabled());
        entity.setMaxPerHour(request.getMaxPerHour());
        entity.setMaxPerDay(request.getMaxPerDay());
        entity.setSilentStart(parseTime(request.getSilentStart()));
        entity.setSilentEnd(parseTime(request.getSilentEnd()));
        entity.setMergeWindowMinutes(request.getMergeWindowMinutes());
        if (Objects.nonNull(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
    }

    private LocalTime parseTime(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        return LocalTime.parse(source);
    }

    private NotificationTaskResponse toResponse(NotificationTask entity) {
        NotificationTaskResponse response = new NotificationTaskResponse();
        response.setTaskId(entity.getTaskId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setUserId(entity.getUserId());
        if (Objects.nonNull(entity.getUserId())) {
            User user = userMapper.selectById(entity.getUserId());
            if (Objects.nonNull(user)) {
                response.setCreatorName(user.getUsername());
            }
        }
        response.setTeamId(entity.getTeamId());
        response.setTriggerType(entity.getTriggerType());
        response.setCronExpression(entity.getCronExpression());
        response.setChannels(entity.getChannels());
        response.setMessageTemplate(entity.getMessageTemplate());
        response.setCustomData(entity.getCustomData());
        response.setPriority(entity.getPriority());
        response.setRateLimitEnabled(entity.getRateLimitEnabled());
        response.setMaxPerHour(entity.getMaxPerHour());
        response.setMaxPerDay(entity.getMaxPerDay());
        response.setSilentStart(Objects.isNull(entity.getSilentStart()) ? null : entity.getSilentStart().toString());
        response.setSilentEnd(Objects.isNull(entity.getSilentEnd()) ? null : entity.getSilentEnd().toString());
        response.setMergeWindowMinutes(entity.getMergeWindowMinutes());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setLastRunAt(entity.getLastRunAt());
        response.setNextRunAt(entity.getNextRunAt());
        response.setStreamEntryId(entity.getStreamEntryId());
        return response;
    }

    private UserContext requireUser() {
        UserContext context = UserContextHolder.get();
        if (Objects.isNull(context)) {
            throw new UnauthorizedException("用户未登录");
        }
        return context;
    }

    private void ensureAccess(NotificationTask entity) {
        UserContext context = requireUser();
        if (context.isAdmin()) {
            return;
        }
        if (Objects.equals(entity.getUserId(), context.getUserId())) {
            return;
        }
        if (context.isTeamAdmin() && Objects.equals(entity.getTeamId(), context.getTeamId())) {
            return;
        }
        throw new AccessDeniedException("没有权限访问任务: " + entity.getTaskId());
    }

    private LambdaQueryWrapper<NotificationTask> scopedQuery() {
        UserContext context = requireUser();
        LambdaQueryWrapper<NotificationTask> wrapper = new LambdaQueryWrapper<>();
        if (context.isAdmin()) {
            return wrapper;
        }
        if (context.isTeamAdmin()) {
            if (Objects.nonNull(context.getTeamId())) {
                wrapper.eq(NotificationTask::getTeamId, context.getTeamId());
            } else {
                wrapper.isNull(NotificationTask::getTeamId);
            }
        } else {
            wrapper.eq(NotificationTask::getUserId, context.getUserId());
        }
        return wrapper;
    }
}
