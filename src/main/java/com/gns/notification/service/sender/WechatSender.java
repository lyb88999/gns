package com.gns.notification.service.sender;

public interface WechatSender {
    void send(String corpId, String corpSecret, String agentId, String toUser, String content);
    void sendViaWebhook(String webhookUrl, String content);
}
