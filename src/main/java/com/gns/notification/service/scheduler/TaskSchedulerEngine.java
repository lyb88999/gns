package com.gns.notification.service.scheduler;

import com.gns.notification.domain.NotificationTask;

public interface TaskSchedulerEngine {
    /**
     * Called when a task is created or updated.
     * Give the engine a chance to schedule it (e.g. add to Redis ZSet).
     * For DB polling, this might do nothing as the poller picks it up automatically.
     */
    void schedule(NotificationTask task);

    /**
     * Called when a task is deleted or disabled.
     */
    void remove(NotificationTask task);
}
