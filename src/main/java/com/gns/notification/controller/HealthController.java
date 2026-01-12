package com.gns.notification.controller;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "service", "universal-notification-system",
            "status", "UP",
            "timestamp", OffsetDateTime.now().toString()
        );
    }
}
