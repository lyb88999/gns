package com.gns.notification.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
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

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStreamEntryId() {
        return streamEntryId;
    }

    public void setStreamEntryId(String streamEntryId) {
        this.streamEntryId = streamEntryId;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }
}
