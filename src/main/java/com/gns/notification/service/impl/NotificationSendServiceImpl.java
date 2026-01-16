package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import com.gns.notification.dto.NotificationSendRequest;
import com.gns.notification.dto.NotificationTaskResponse;
import com.gns.notification.exception.RateLimitExceededException;
import com.gns.notification.exception.UnauthorizedException;
import com.gns.notification.security.UserContext;
import com.gns.notification.security.UserContextHolder;
import com.gns.notification.service.NotificationSendService;
import com.gns.notification.service.NotificationTaskService;

import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class NotificationSendServiceImpl implements NotificationSendService {

    private final NotificationTaskService taskService;
    private final NotificationTaskMapper taskMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String streamKey;

    public NotificationSendServiceImpl(@org.springframework.context.annotation.Lazy NotificationTaskService taskService,
                                       NotificationTaskMapper taskMapper,
                                       RedisTemplate<String, Object> redisTemplate,
                                       @Value("${app.notification.redis-stream-key}") String streamKey) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
    }

    @Override
    public NotificationTaskResponse send(NotificationSendRequest request) {
        UserContext ctx = Optional.ofNullable(UserContextHolder.get())
                .orElseThrow(() -> new UnauthorizedException("用户未登录"));

        NotificationTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<NotificationTask>()
                        .eq(NotificationTask::getTaskId, request.getTaskId())
        );
        if (Objects.isNull(task)) {
            throw new IllegalArgumentException("Task not found: " + request.getTaskId());
        }
        // 基础权限：管理员或同队/本人
        if (!ctx.isAdmin()) {
            if (!ctx.isTeamAdmin() || (Objects.nonNull(task.getTeamId()) && !task.getTeamId().equals(ctx.getTeamId()))) {
                if (!task.getUserId().equals(ctx.getUserId())) {
                    throw new UnauthorizedException("没有权限发送该任务");
                }
            }
        }

        checkSilentMode(task);
        checkRateLimit(task);

        Map<Object, Object> payload = new HashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("data", request.getData());
        payload.put("priority", StringUtils.hasText(request.getPriority()) ? request.getPriority() : task.getPriority());
        payload.put("attachments", request.getAttachments());
        payload.put("requestedAt", Instant.now().toString());
        payload.put("userId", ctx.getUserId());
        payload.put("teamId", ctx.getTeamId());

        MapRecord<String, Object, Object> record = MapRecord.create(streamKey, payload);
        RecordId id = redisTemplate.opsForStream().add(record);
        taskService.markStreamEntry(task.getTaskId(), id.getValue());
        return taskService.getTask(task.getTaskId());
    }

    private void checkRateLimit(NotificationTask task) {
        if (Boolean.TRUE.equals(task.getRateLimitEnabled())) {
            String hourKey = "gns:limit:task:" + task.getTaskId() + ":hour:" + Instant.now().getEpochSecond() / 3600;
            String dayKey = "gns:limit:task:" + task.getTaskId() + ":day:" + Instant.now().getEpochSecond() / 86400;

            if (task.getMaxPerHour() != null && task.getMaxPerHour() > 0) {
                Long count = redisTemplate.opsForValue().increment(hourKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(hourKey, 1, TimeUnit.HOURS);
                }
                if (count != null && count > task.getMaxPerHour()) {
                    throw new RateLimitExceededException("Rate limit exceeded (Hour): " + task.getMaxPerHour());
                }
            }

            if (task.getMaxPerDay() != null && task.getMaxPerDay() > 0) {
                Long count = redisTemplate.opsForValue().increment(dayKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(dayKey, 24, TimeUnit.HOURS);
                }
                if (count != null && count > task.getMaxPerDay()) {
                    throw new RateLimitExceededException("Rate limit exceeded (Day): " + task.getMaxPerDay());
                }
            }
        }
    }


    private void checkSilentMode(NotificationTask task) {
        if (Objects.nonNull(task.getSilentStart()) && Objects.nonNull(task.getSilentEnd())) {
            // Fix: Use Asia/Shanghai timezone to match user expectation
            LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Shanghai"));
            boolean isSilent = isSilent(task, now);

            if (isSilent) {
                throw new RateLimitExceededException("Silent Mode is active (" + task.getSilentStart() + " - " + task.getSilentEnd() + ")");
            }
        }
    }

    private static boolean isSilent(NotificationTask task, LocalTime now) {
        boolean isSilent = false;
        if (task.getSilentStart().isBefore(task.getSilentEnd())) {
            // Same day, e.g., 09:00 to 18:00
            if (!now.isBefore(task.getSilentStart()) && !now.isAfter(task.getSilentEnd())) {
                isSilent = true;
            }
        } else {
            // Cross day, e.g., 22:00 to 07:00
            if (!now.isBefore(task.getSilentStart()) || !now.isAfter(task.getSilentEnd())) {
                isSilent = true;
            }
        }
        return isSilent;
    }
}
