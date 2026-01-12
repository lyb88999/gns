package com.gns.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalNotifications;
    private double successRate;
    private long activeTasks;
    private List<VolumeData> notificationVolume24h;

    @Data
    @Builder
    public static class VolumeData {
        private String time;
        private long count;
    }
}
