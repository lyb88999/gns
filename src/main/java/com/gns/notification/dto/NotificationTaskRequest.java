package com.gns.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

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

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    public void setCustomData(Map<String, Object> customData) {
        this.customData = customData;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Boolean getRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(Boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    public Integer getMaxPerHour() {
        return maxPerHour;
    }

    public void setMaxPerHour(Integer maxPerHour) {
        this.maxPerHour = maxPerHour;
    }

    public Integer getMaxPerDay() {
        return maxPerDay;
    }

    public void setMaxPerDay(Integer maxPerDay) {
        this.maxPerDay = maxPerDay;
    }

    public String getSilentStart() {
        return silentStart;
    }

    public void setSilentStart(String silentStart) {
        this.silentStart = silentStart;
    }

    public String getSilentEnd() {
        return silentEnd;
    }

    public void setSilentEnd(String silentEnd) {
        this.silentEnd = silentEnd;
    }

    public Integer getMergeWindowMinutes() {
        return mergeWindowMinutes;
    }

    public void setMergeWindowMinutes(Integer mergeWindowMinutes) {
        this.mergeWindowMinutes = mergeWindowMinutes;
    }
}
