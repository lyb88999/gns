package com.gns.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class NotificationTaskRequest {

    private String taskId;

    @NotBlank
    private String name;

    @Size(max = 500)
    private String description;

    @NotBlank
    private String triggerType;

    private String cronExpression;

    @NotEmpty
    private List<String> channels;

    @NotBlank
    private String messageTemplate;

    private String priority;

    private Map<String, Object> customData;
    private Boolean rateLimitEnabled = Boolean.TRUE;
    private Integer maxPerHour = 10;
    private Integer maxPerDay = 100;
    private String silentStart;
    private String silentEnd;
    private Integer mergeWindowMinutes = 5;
    private Boolean status;

}
