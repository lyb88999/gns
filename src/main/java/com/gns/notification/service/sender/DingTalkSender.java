package com.gns.notification.service.sender;

public interface DingTalkSender {
    void send(String webhook, String secret, String content) throws Exception;
}
