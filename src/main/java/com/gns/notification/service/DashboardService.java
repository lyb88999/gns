package com.gns.notification.service;

import com.gns.notification.dto.DashboardStatsResponse;

public interface DashboardService {
    DashboardStatsResponse getStats(String granularity);
}
