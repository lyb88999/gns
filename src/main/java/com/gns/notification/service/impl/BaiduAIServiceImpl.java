package com.gns.notification.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gns.notification.dto.ai.TaskIntentResponse;
import com.gns.notification.service.AIService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaiduAIServiceImpl implements AIService {

    @Value("${ai.baidu.api-key:}")
    private String apiKey;

    // Direct endpoint for ERNIE-Speed-128K
    // Note: The new Auth method (Bearer) uses global endpoint usually? 
    // Doc says: https://qianfan.baidubce.com/v2/chat/completions
    private static final String CHAT_URL = "https://qianfan.baidubce.com/v2/chat/completions";

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    @Override
    public TaskIntentResponse parseTaskIntent(String prompt) {
        if (!hasValidConfig()) {
            throw new RuntimeException("Baidu AI credentials not configured. Please set ai.baidu.api-key");
        }

        // System Prompt to guide the model to output strict JSON
        String systemPrompt = """
            You are a smart notification scheduler assistant.
            Extract task details from the user's natural language input and return a STRICT JSON object.
            Do not output any markdown code blocks (like ```json), just the raw JSON string.

            ### Target JSON Structure:
            {
                "name": "Task Name",
                "cron": "Quartz Cron Expression (e.g. '0 0 9 * * ?'). Set to null or empty string if it is a manual/API trigger.",
                "channels": ["Channel1", "Channel2"],
                "recipient": "Recipient details",
                "subject": "Email Subject (if applicable)",
                "template": "Message content with ${placeholder} if needed",
                "explanation": "Brief explanation"
            }

            ### Few-Shot Examples (Learn from these):

            User: "帮我生成一个每天晚上7点通过钉钉+企业微信通知我的任务"
            AI: {
                "name": "每日晚间通知",
                "cron": "0 0 19 * * ?",
                "channels": ["DingTalk", "WeChat"],
                "recipient": null,
                "template": "晚上好，现在是7点整。",
                "explanation": "创建每天19:00触发的任务，同时推送到钉钉和企业微信。"
            }

            User: "创建一个手动触发的任务，发送给我的邮箱，告诉任务结束了"
            AI: {
                "name": "任务结束通知",
                "cron": "",
                "channels": ["Email"],
                "recipient": "User Email",
                "subject": "任务结束通知",
                "template": "任务已结束。",
                "explanation": "创建手动触发(API)的任务，渠道为邮件。"
            }

            Current Time: %s
            """.formatted(LocalDateTime.now());

        BaiduChatRequest request = new BaiduChatRequest();
        request.setModel("ernie-speed-128k"); // Model name for V2 API
        request.setMessages(List.of(
            new BaiduMessage("user", systemPrompt + "\n\nUser Input: " + prompt)
        ));
        request.setTemperature(0.1); 

        try {
            String responseBody = restClient.post()
                    .uri(CHAT_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                 // V2 API error format might be different
                 JsonNode error = root.get("error");
                 throw new RuntimeException("Baidu API Error: " + error.get("message").asText());
            }

            // V2 response structure: choices[0].message.content
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content.isEmpty()) {
                throw new RuntimeException("Empty response from AI");
            }
            content = cleanJson(content);
            
            return objectMapper.readValue(content, TaskIntentResponse.class);

        } catch (Exception e) {
            log.error("Failed to parse task intent with Baidu AI", e);
            throw new RuntimeException("AI Parsing Failed: " + e.getMessage());
        }
    }

    private boolean hasValidConfig() {
        return apiKey != null && !apiKey.isEmpty();
    }
    
    private String cleanJson(String content) {
        Pattern pattern = Pattern.compile("```json?(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content.trim();
    }

    @Data
    static class BaiduChatRequest {
        private String model;
        private List<BaiduMessage> messages;
        private double temperature = 0.8;
    }

    @Data
    @RequiredArgsConstructor
    static class BaiduMessage {
        private final String role;
        private final String content;
    }
}
