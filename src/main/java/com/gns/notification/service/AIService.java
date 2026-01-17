package com.gns.notification.service;

import com.gns.notification.dto.ai.TaskIntentResponse;

public interface AIService {
    TaskIntentResponse parseTaskIntent(String prompt);
}
