package com.gns.notification.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@TableName(value = "notification_logs", autoResultMap = true)
public class NotificationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String notificationId;
    private String taskId;
    private String taskName;
    private Long userId;
    private String channel;
    private String recipient;
    private String subject;
    private String content;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> customData;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> attachments;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

}
