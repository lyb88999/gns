package com.gns.notification.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@TableName(value = "notification_tasks", autoResultMap = true)
public class NotificationTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String name;
    private String description;
    private Long userId;
    private Long teamId;
    private String triggerType;
    private String cronExpression;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> channels;
    private String messageTemplate;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> customData;
    private String priority;
    private Boolean rateLimitEnabled;
    private Integer maxPerHour;
    private Integer maxPerDay;
    private LocalTime silentStart;
    private LocalTime silentEnd;
    private Integer mergeWindowMinutes;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private String streamEntryId;

}
