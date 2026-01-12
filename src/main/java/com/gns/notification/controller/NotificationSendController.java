package com.gns.notification.controller;

import com.gns.notification.dto.NotificationSendRequest;
import com.gns.notification.dto.NotificationTaskResponse;
import com.gns.notification.service.NotificationSendService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notify")
public class NotificationSendController {

    private final NotificationSendService notificationSendService;

    public NotificationSendController(NotificationSendService notificationSendService) {
        this.notificationSendService = notificationSendService;
    }

    @PostMapping
    public ResponseEntity<NotificationTaskResponse> send(@Valid @RequestBody NotificationSendRequest request) {
        return ResponseEntity.ok(notificationSendService.send(request));
    }
}
