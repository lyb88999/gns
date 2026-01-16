package com.gns.notification.service.strategy;

import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.User;
import java.util.List;
import java.util.Map;

public interface NotificationChannelStrategy {

    /**
     * The channel name this strategy supports (e.g., "Email", "DingTalk", "WeChat")
     */
    String getChannelName();

    /**
     * Execute the sending logic.
     * @return DispatchResult containing status, recipient, and error message (if any)
     */
    List<DispatchResult> send(NotificationTask task, String content, User user, Map<String, Object> data);
}
