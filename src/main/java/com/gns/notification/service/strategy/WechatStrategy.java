package com.gns.notification.service.strategy;

import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.User;
import com.gns.notification.service.sender.WechatSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class WechatStrategy implements NotificationChannelStrategy {

    private final WechatSender wechatSender;

    public WechatStrategy(WechatSender wechatSender) {
        this.wechatSender = wechatSender;
    }

    @Override
    public String getChannelName() {
        return "WeChat";
    }

    @Override
    public List<DispatchResult> send(NotificationTask task, String content, User user, Map<String, Object> data) {
        if (Objects.isNull(task.getCustomData())) {
            return  Collections.singletonList(DispatchResult.ignored());
        }

        // 1. Try Webhook first (Robot Mode)
        String webhook = (String) task.getCustomData().get("wechatWebhook");
        if (Objects.nonNull(webhook) && !webhook.isEmpty()) {
             try {
                wechatSender.sendViaWebhook(webhook, content);
                log.info("Sent WeChat Webhook message to {}", webhook);
                return Collections.singletonList(DispatchResult.success(webhook));
            } catch (Exception e) {
                log.error("Failed to send WeChat message via webhook: {}", webhook, e);
                return Collections.singletonList(DispatchResult.failure(webhook, e.getMessage()));
            }
        }
        
        // 2. Fallback to App Mode
        String corpId = (String) task.getCustomData().get("wechatCorpId");
        String secret = (String) task.getCustomData().get("wechatCorpSecret");
        String agentId = (String) task.getCustomData().get("wechatAgentId");

        // Determine recipient: 1. data payload 2. task config 3. default @all
        String toUser = Objects.nonNull(data.get("wechatUser"))
                ? String.valueOf(data.get("wechatUser"))
                : (String) task.getCustomData().get("wechatToUser");

        if (Objects.nonNull(corpId) && Objects.nonNull(secret) && Objects.nonNull(agentId)) {
            try {
                wechatSender.send(corpId, secret, agentId, toUser, content);
                log.info("Sent WeChat App message to user: {}", toUser);
                return Collections.singletonList(DispatchResult.success(toUser));
            } catch (Exception e) {
                log.error("Failed to send WeChat message via App: {}", toUser, e);
                return Collections.singletonList(DispatchResult.failure(toUser, e.getMessage()));
            }
        } else {
            return Collections.singletonList(DispatchResult.failure(toUser, "Missing WeChat configuration. Provide either Webhook URL OR CorpId/Secret/AgentId"));
        }
    }
}
