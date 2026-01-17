package com.gns.notification.controller;

import com.gns.notification.dto.ai.TaskIntentRequest;
import com.gns.notification.dto.ai.TaskIntentResponse;
import com.gns.notification.service.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/parse-task")
    public ResponseEntity<TaskIntentResponse> parseTask(@RequestBody TaskIntentRequest request) {
        return ResponseEntity.ok(aiService.parseTaskIntent(request.getPrompt()));
    }
}
