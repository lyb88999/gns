package com.gns.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class DingTalkSender {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DingTalkSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void send(String webhook, String secret, String content) {
        if (!StringUtils.hasText(webhook)) {
            throw new IllegalArgumentException("DingTalk Webhook URL is required");
        }

        try {
            String targetUrl = webhook;
            if (StringUtils.hasText(secret)) {
                long timestamp = System.currentTimeMillis();
                String stringToSign = timestamp + "\n" + secret;
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
                String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
                
                String separator = webhook.contains("?") ? "&" : "?";
                targetUrl = webhook + separator + "timestamp=" + timestamp + "&sign=" + sign;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "text");
            Map<String, String> text = new HashMap<>();
            text.put("content", content);
            payload.put("text", text);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            System.err.println("Failed to send DingTalk message: " + response.body());
                        }
                    });

        } catch (Exception e) {
            throw new RuntimeException("Failed to send DingTalk message", e);
        }
    }
}
