package com.gns.notification.service;
import com.gns.notification.service.sender.WechatSender;
import com.gns.notification.service.impl.WechatSenderImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WechatSenderTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpResponse<String> httpResponse;

    private WechatSender wechatSender;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        wechatSender = new WechatSenderImpl(objectMapper, redisTemplate, httpClient);
    }

    @Test
    void send_ShouldSuccess_WhenTokenCached() throws Exception {
        // Arrange
        String corpId = "corp1";
        String secret = "sec1";
        String token = "cached_token";
        
        when(valueOperations.get("gns:wechat:token:" + corpId)).thenReturn(token);
        // Mock send for message (sync)
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"errcode\":0,\"errmsg\":\"ok\"}");

        // Act & Assert
        assertDoesNotThrow(() -> wechatSender.send(corpId, secret, "1001", "user1", "Hello"));

        // Verify no gettoken call, only send message
        // Since we cannot easily distinguish requests without argument captors, we just verify call count.
        // But getAccessToken is not called, so only 1 send.
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    void send_ShouldFetchToken_WhenCacheMiss() throws Exception {
        // Arrange
        String corpId = "corp1";
        String secret = "sec1";
        
        // Cache miss
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // Mock Token Response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"errcode\":0,\"errmsg\":\"ok\",\"access_token\":\"new_token\",\"expires_in\":7200}");
        
        // Mock Send Response (reuse httpResponse mock)
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"errcode\":0,\"errmsg\":\"ok\"}");

        // Important: httpClient.send is called twice (1. token, 2. message)
        // We need to return different responses based on invocation sequence or arguments.
        // Using Answer or consecutive returns.
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(tokenResponse) // First call: Token
            .thenReturn(httpResponse); // Second call: Message
            
        // Act
        wechatSender.send(corpId, secret, "1001", "user1", "Hello");

        // Verify token fetched and cached
        verify(httpClient, times(2)).send(any(), any()); // Token + Message
        verify(valueOperations).set(eq("gns:wechat:token:" + corpId), eq("new_token"), anyLong(), eq(TimeUnit.SECONDS));
    }
}
