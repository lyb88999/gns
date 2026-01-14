package com.gns.notification.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class NotificationTaskResponse {

    private String taskId;
    private String name;
    private String description;
    private Long userId;
    private Long teamId;
    private String creatorName;
    private String triggerType;
    private String cronExpression;
    private List<String> channels;
    private String messageTemplate;
    private Map<String, Object> customData;
    private String priority;
    private Boolean rateLimitEnabled;
    private Integer maxPerHour;
    private Integer maxPerDay;
    private String silentStart;
    private String silentEnd;
    private Integer mergeWindowMinutes;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private String streamEntryId;

}
