package com.gns.notification.controller;

import com.gns.notification.dto.NotificationLogResponse;
import com.gns.notification.dto.PageResult;
import com.gns.notification.service.NotificationLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public class NotificationLogController {

    private final NotificationLogService notificationLogService;

    public NotificationLogController(NotificationLogService notificationLogService) {
        this.notificationLogService = notificationLogService;
    }

    @GetMapping
    public ResponseEntity<PageResult<NotificationLogResponse>> listLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(notificationLogService.listLogs(PageRequest.of(page, size), status, search));
    }
}
