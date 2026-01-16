package com.gns.notification.service.impl;
import com.gns.notification.service.sender.WechatSender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WechatSenderImpl implements WechatSender {

    private static final Logger logger = LoggerFactory.getLogger(WechatSenderImpl.class);
    private static final String API_HOST = "https://qyapi.weixin.qq.com";
    private static final String TOKEN_KEY_PREFIX = "gns:wechat:token:";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public WechatSenderImpl(ObjectMapper objectMapper, StringRedisTemplate redisTemplate, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.httpClient = httpClient;
    }

    public void send(String corpId, String corpSecret, String agentId, String toUser, String content) {
        if (!StringUtils.hasText(corpId) || !StringUtils.hasText(corpSecret) || !StringUtils.hasText(agentId)) {
            throw new IllegalArgumentException("WeChat CorpId, CorpSecret, and AgentId are required");
        }
        try {
            String accessToken = getAccessToken(corpId, corpSecret);
            String url = API_HOST + "/cgi-bin/message/send?access_token=" + accessToken;
            postMessage(url, buildAppPayload(agentId, toUser, content));
        } catch (Exception e) {
            throw new RuntimeException("Failed to send WeChat App message", e);
        }
    }

    public void sendViaWebhook(String webhookUrl, String content) {
        if (!StringUtils.hasText(webhookUrl)) {
            throw new IllegalArgumentException("WeChat Webhook URL is required");
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "text");
            Map<String, String> text = new HashMap<>();
            text.put("content", content);
            payload.put("text", text);
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            postMessage(webhookUrl.trim(), jsonPayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send WeChat Webhook message", e);
        }
    }

    private String buildAppPayload(String agentId, String toUser, String content) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("touser", StringUtils.hasText(toUser) ? toUser : "@all");
        payload.put("msgtype", "text");
        payload.put("agentid", Integer.parseInt(agentId));
        Map<String, String> text = new HashMap<>();
        text.put("content", content);
        payload.put("text", text);
        return objectMapper.writeValueAsString(payload);
    }

    private void postMessage(String url, String jsonPayload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP Error " + response.statusCode() + ": " + response.body());
            }

            JsonNode responseNode = objectMapper.readTree(response.body());
            if (responseNode.has("errcode") && responseNode.get("errcode").asInt() != 0) {
                String errorMsg = "WeChat API error: " + response.body();
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to send WeChat message", e);
        }
    }

    private String getAccessToken(String corpId, String corpSecret) {
        String key = TOKEN_KEY_PREFIX + corpId;
        String cachedToken = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cachedToken)) {
            return cachedToken;
        }

        // Fetch new token
        String url = API_HOST + "/cgi-bin/gettoken?corpid=" + corpId + "&corpsecret=" + corpSecret;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("errcode") && root.get("errcode").asInt() != 0) {
                    throw new RuntimeException("WeChat Token Error: " + response.body());
                }
                
                String token = root.get("access_token").asText();
                int expiresIn = root.get("expires_in").asInt();
                
                // Cache with safety margin
                redisTemplate.opsForValue().set(key, token, Math.max(expiresIn - 200, 60), TimeUnit.SECONDS);
                return token;
            } else {
                throw new RuntimeException("Failed to fetch WeChat token, HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get WeChat access token", e);
        }
    }
}
