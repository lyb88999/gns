package com.gns.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationStatus {
    PENDING("pending"),
    SUCCESS("success"),
    FAILED("failed"),
    BLOCKED("blocked");

    @EnumValue
    @JsonValue
    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
