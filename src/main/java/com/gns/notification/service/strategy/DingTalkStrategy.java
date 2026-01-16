package com.gns.notification.service.strategy;

import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.User;
import com.gns.notification.service.sender.DingTalkSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class DingTalkStrategy implements NotificationChannelStrategy {

    private final DingTalkSender dingTalkSender;

    public DingTalkStrategy(DingTalkSender dingTalkSender) {
        this.dingTalkSender = dingTalkSender;
    }

    @Override
    public String getChannelName() {
        return "DingTalk";
    }

    @Override
    public List<DispatchResult> send(NotificationTask task, String content, User user, Map<String, Object> data) {
        if (Objects.isNull(task.getCustomData())) {
            return Collections.singletonList(DispatchResult.ignored());
        }
        String webhook = (String) task.getCustomData().get("dingTalkWebhook");
        String secret = (String) task.getCustomData().get("dingTalkSecret");
        
        if (Objects.isNull(webhook)) {
            return Collections.singletonList(DispatchResult.failure(null, "Missing webhook URL"));
        }

        try {
            dingTalkSender.send(webhook, secret, content);
            log.info("Sent DingTalk message to webhook: {}", webhook);
            return Collections.singletonList(DispatchResult.success(webhook));
        } catch (Exception e) {
            log.error("Failed to send DingTalk message via webhook: {}", webhook, e);
            return Collections.singletonList(DispatchResult.failure(webhook, e.getMessage()));
        }
    }
}
