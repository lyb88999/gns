package com.gns.notification.dto;

import com.gns.notification.domain.NotificationLog;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalNotifications;
    private double successRate;
    private long activeTasks;
    private Map<String, Long> channelDistribution;
    private Map<String, Long> statusDistribution;
    private List<NotificationLog> recentLogs;
    private List<ErrorStat> topErrors;
    private List<VolumeData> notificationVolume24h;

    @Data
    @Builder
    public static class ErrorStat {
        private String errorMessage;
        private Long count;
    }

    @Data
    @Builder
    public static class VolumeData {
        private String time;
        private long count;
    }
}
