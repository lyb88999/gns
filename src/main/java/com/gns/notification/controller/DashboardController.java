package com.gns.notification.controller;

import com.gns.notification.dto.DashboardStatsResponse;
import com.gns.notification.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "5m") String granularity) {
        return ResponseEntity.ok(dashboardService.getStats(granularity));
    }
}
