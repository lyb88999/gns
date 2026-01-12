package com.gns.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationLogResponse {
    private Long id;
    private String taskId;
    private String taskName;
    private String channel;
    private String recipient;
    private String status;
    private String errorMessage;
    private LocalDateTime sentAt;
}
