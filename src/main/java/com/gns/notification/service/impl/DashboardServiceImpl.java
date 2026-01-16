package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gns.notification.domain.NotificationLog;
import com.gns.notification.domain.NotificationLogMapper;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import com.gns.notification.dto.DashboardStatsResponse;
import com.gns.notification.enums.NotificationStatus;
import com.gns.notification.security.UserContext;
import com.gns.notification.security.UserContextHolder;
import com.gns.notification.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final NotificationLogMapper logMapper;
    private final NotificationTaskMapper taskMapper;



    private QueryWrapper<NotificationLog> buildLogWrapper() {
        UserContext ctx = UserContextHolder.get();
        QueryWrapper<NotificationLog> wrapper = new QueryWrapper<>();
        
        if (Objects.isNull(ctx)) {
            throw new com.gns.notification.exception.UnauthorizedException("User context is missing");
        }
        if (ctx.isAdmin()) {
            return wrapper;
        }
        // Use lambda to ensure correct column mapping
        wrapper.lambda().eq(NotificationLog::getUserId, ctx.getUserId());
        return wrapper;
    }

    private QueryWrapper<NotificationTask> buildTaskWrapper() {
        UserContext ctx = UserContextHolder.get();
        QueryWrapper<NotificationTask> wrapper = new QueryWrapper<>();
        
        if (Objects.isNull(ctx)) {
            throw new com.gns.notification.exception.UnauthorizedException("User context is missing");
        }
        if (ctx.isAdmin()) {
            return wrapper;
        }
        if (ctx.isTeamAdmin()) {
            wrapper.lambda().eq(NotificationTask::getTeamId, ctx.getTeamId());
        } else {
            wrapper.lambda().eq(NotificationTask::getUserId, ctx.getUserId());
        }
        return wrapper;
    }

    @Override
    public DashboardStatsResponse getStats(String granularity) {
        // 1. Total Notifications (Excluding SKIPPED)
        QueryWrapper<NotificationLog> totalWrapper = buildLogWrapper();
        totalWrapper.lambda().ne(NotificationLog::getStatus, "skipped");
        Long totalNotifications = logMapper.selectCount(totalWrapper);

        // 2. Success Rate
        QueryWrapper<NotificationLog> successWrapper = buildLogWrapper();
        successWrapper.lambda().and(w -> w.eq(NotificationLog::getStatus, NotificationStatus.SUCCESS.getValue())
                                 .or()
                                 .eq(NotificationLog::getStatus, "SUCCESS"));
        Long successCount = logMapper.selectCount(successWrapper);
        double successRate = totalNotifications > 0 ? (double) successCount / totalNotifications * 100 : 0;

        // 3. Active Tasks
        QueryWrapper<NotificationTask> taskWrapper = buildTaskWrapper();
        taskWrapper.lambda().eq(NotificationTask::getStatus, true);
        Long activeTasks = taskMapper.selectCount(taskWrapper);

        // -- New Metrics --

        // 4. Channel Distribution
        QueryWrapper<NotificationLog> channelWrapper = buildLogWrapper();
        channelWrapper.select("channel", "count(*) as count")
                      .groupBy("channel");
        List<Map<String, Object>> channelStats = logMapper.selectMaps(channelWrapper);
        Map<String, Long> channelDistribution = new HashMap<>();
        if (Objects.nonNull(channelStats)) {
            for (Map<String, Object> map : channelStats) {
                String channel = (String) map.get("channel");
                Long count = ((Number) map.get("count")).longValue();
                if (Objects.nonNull(channel)) {
                    channelDistribution.put(channel, count);
                }
            }
        }

        // 5. Status Distribution (Success, Failed, Blocked, Skipped)
        QueryWrapper<NotificationLog> statusWrapper = buildLogWrapper();
        statusWrapper.select("status", "count(*) as count")
                     .groupBy("status");
        List<Map<String, Object>> statusStats = logMapper.selectMaps(statusWrapper);
        Map<String, Long> statusDistribution = new HashMap<>();
        if (Objects.nonNull(statusStats)) {
            for (Map<String, Object> map : statusStats) {
                String status = (String) map.get("status");
                Long count = ((Number) map.get("count")).longValue();
                if (Objects.nonNull(status)) {
                    statusDistribution.put(status, count);
                }
            }
        }

        // 6. Recent Logs (Top 5)
        QueryWrapper<NotificationLog> recentLogsWrapper = buildLogWrapper();
        recentLogsWrapper.orderByDesc("created_at")
                         .last("LIMIT 5");
        List<NotificationLog> recentLogs = logMapper.selectList(recentLogsWrapper);
        QueryWrapper<NotificationLog> errorWrapper = buildLogWrapper();
        errorWrapper.select("error_message", "count(*) as count")
                    .isNotNull("error_message")
                    .ne("error_message", "")
                    .groupBy("error_message")
                    .orderByDesc("count")
                    .last("LIMIT 5");
        List<Map<String, Object>> errorStats = logMapper.selectMaps(errorWrapper);
        List<DashboardStatsResponse.ErrorStat> topErrors = new ArrayList<>();
        if (errorStats != null) {
            for (Map<String, Object> map : errorStats) {
                String errorMsg = (String) map.get("error_message");
                Long count = ((Number) map.get("count")).longValue();
                topErrors.add(DashboardStatsResponse.ErrorStat.builder()
                        .errorMessage(errorMsg)
                        .count(count)
                        .build());
            }
        }

        // 8. Volume 24h
        // Determine interval in minutes
        int intervalMinutes = 5;
        if (granularity != null && !granularity.isEmpty()) {
            String g = granularity.toLowerCase();
            try {
                if (g.endsWith("h")) {
                    intervalMinutes = Integer.parseInt(g.replace("h", "")) * 60;
                } else if (g.endsWith("m")) {
                    intervalMinutes = Integer.parseInt(g.replace("m", ""));
                }
            } catch (NumberFormatException e) {
                // ignore, keep default
            }
        }
        if (intervalMinutes < 1) intervalMinutes = 5;
        
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        QueryWrapper<NotificationLog> recentWrapper = buildLogWrapper();
        recentWrapper.ge("created_at", twentyFourHoursAgo);
        
        final int finalIntervalMinutes = intervalMinutes;

        List<NotificationLog> recentLogs24h = logMapper.selectList(recentWrapper);

        Map<String, Long> volumeMap = recentLogs24h.stream()
                .collect(Collectors.groupingBy(
                        log -> {
                            LocalDateTime t = log.getCreatedAt();
                            int minute = (t.getMinute() / finalIntervalMinutes) * finalIntervalMinutes;
                            return t.withMinute(minute).withSecond(0).withNano(0)
                                    .format(DateTimeFormatter.ofPattern("HH:mm"));
                        },
                        Collectors.counting()
                ));

        List<DashboardStatsResponse.VolumeData> volumeList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // Round 'now' down to nearest interval to match the buckets
        int currentMinute = (now.getMinute() / intervalMinutes) * intervalMinutes;
        LocalDateTime endTime = now.withMinute(currentMinute).withSecond(0).withNano(0);
        
        // 24 hours * (60 / interval)
        int totalIntervals = 24 * (60 / intervalMinutes);
        for (int i = totalIntervals; i >= 0; i--) {
            LocalDateTime timePoint = endTime.minusMinutes(i * intervalMinutes);
            String timeLabel = timePoint.format(DateTimeFormatter.ofPattern("HH:mm"));
            volumeList.add(DashboardStatsResponse.VolumeData.builder()
                    .time(timeLabel)
                    .count(volumeMap.getOrDefault(timeLabel, 0L))
                    .build());
        }

        return DashboardStatsResponse.builder()
                .totalNotifications(totalNotifications)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .activeTasks(activeTasks)
                .channelDistribution(channelDistribution)
                .statusDistribution(statusDistribution)
                .recentLogs(recentLogs)
                .topErrors(topErrors)
                .notificationVolume24h(volumeList)
                .build();
    }
}
