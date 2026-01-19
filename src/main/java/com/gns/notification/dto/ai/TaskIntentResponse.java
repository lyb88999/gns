package com.gns.notification.dto.ai;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TaskIntentResponse {
    private String name;
    private String cron;
    private List<String> channels;
    private String recipient;
    private String subject;
    private String template;
    private String explanation;
}
