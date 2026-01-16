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

@Setter
@Getter
@TableName(value = "api_tokens", autoResultMap = true)
public class ApiToken {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String token;
    private String name;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> scopes;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

}
