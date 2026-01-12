package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gns.notification.domain.NotificationLog;
import com.gns.notification.domain.NotificationLogMapper;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import com.gns.notification.dto.DashboardStatsResponse;
import com.gns.notification.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final NotificationLogMapper logMapper;
    private final NotificationTaskMapper taskMapper;



    private com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NotificationLog> buildLogWrapper() {
        com.gns.notification.security.UserContext ctx = com.gns.notification.security.UserContextHolder.get();
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NotificationLog> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        
        if (ctx == null || ctx.isAdmin()) {
            return wrapper;
        }
        if (ctx.isTeamAdmin()) {
            // Logs don't strictly have team_id column, but they have user_id. 
            // Ideally logs should have team_id. 
            // For now, if logs table doesn't have team_id, Team Admin might only see their own or we need to join users.
            // Let's assume for now Team Admin sees their own logs or we skip complex team log filtering if column missing.
            // Wait, previous investigation showed we rely on user_id.
            // Let's stick to user_id for non-admins for now to be safe, or if Team Admin, we might need to find all users in team.
            // Limitation: Without team_id in logs, Team Admin stats might be limited.
            // Let's check NotificationLog entity again. It has userId.
            // Strategy: For simplicity and correctness with current schema:
            // User -> see own logs.
            // Team Admin -> see own logs (unless we do join). 
            // Let's restrict to userId for both for now to avoid cross-team data leak until schema supports team_id on logs.
            wrapper.eq("user_id", ctx.getUserId());
        } else {
            wrapper.eq("user_id", ctx.getUserId());
        }
        return wrapper;
    }

    private com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NotificationTask> buildTaskWrapper() {
        com.gns.notification.security.UserContext ctx = com.gns.notification.security.UserContextHolder.get();
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NotificationTask> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        
        if (ctx == null || ctx.isAdmin()) {
            return wrapper;
        }
        if (ctx.isTeamAdmin()) {
            wrapper.eq("team_id", ctx.getTeamId());
        } else {
            wrapper.eq("user_id", ctx.getUserId());
        }
        return wrapper;
    }

    @Override
    public DashboardStatsResponse getStats() {
        // 1. Total Notifications
        Long totalNotifications = logMapper.selectCount(buildLogWrapper());

        // 2. Success Rate
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NotificationLog> successWrapper = buildLogWrapper();
        successWrapper.and(w -> w.eq("status", com.gns.notification.enums.NotificationStatus.SUCCESS.getValue())
                                 .or()
                                 .eq("status", "SUCCESS"));
        Long successCount = logMapper.selectCount(successWrapper);
        double successRate = totalNotifications > 0 ? (double) successCount / totalNotifications * 100 : 0;

        // 3. Active Tasks
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NotificationTask> taskWrapper = buildTaskWrapper();
        taskWrapper.eq("status", true);
        Long activeTasks = taskMapper.selectCount(taskWrapper);

        // 4. Volume 24h
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NotificationLog> recentWrapper = buildLogWrapper();
        recentWrapper.ge("created_at", twentyFourHoursAgo);
        
        List<NotificationLog> recentLogs = logMapper.selectList(recentWrapper);

        Map<String, Long> volumeMap = recentLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:00")),
                        Collectors.counting()
                ));

        List<DashboardStatsResponse.VolumeData> volumeList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 23; i >= 0; i--) {
            String timeLabel = now.minusHours(i).format(DateTimeFormatter.ofPattern("HH:00"));
            volumeList.add(DashboardStatsResponse.VolumeData.builder()
                    .time(timeLabel)
                    .count(volumeMap.getOrDefault(timeLabel, 0L))
                    .build());
        }

        return DashboardStatsResponse.builder()
                .totalNotifications(totalNotifications)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .activeTasks(activeTasks)
                .notificationVolume24h(volumeList)
                .build();
    }
}
