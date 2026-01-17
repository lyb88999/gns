package com.gns.notification.dto.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskIntentResponse {
    private String name;
    private String cron;
    private String channel;
    private String recipient;
    private String subject;
    private String template;
    private String explanation;
}
