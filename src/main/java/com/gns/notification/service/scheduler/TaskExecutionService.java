package com.gns.notification.service.scheduler;

import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class TaskExecutionService {

    private final NotificationTaskMapper notificationTaskMapper;
    private final TaskCoreProcessor taskCoreProcessor;

    public TaskExecutionService(NotificationTaskMapper notificationTaskMapper,
                                TaskCoreProcessor taskCoreProcessor) {
        this.notificationTaskMapper = notificationTaskMapper;
        this.taskCoreProcessor = taskCoreProcessor;
    }

    /**
     * Used by Schedulers (Redis/DB)
     */
    public void executeTask(NotificationTask task, boolean isManual) {
        taskCoreProcessor.executeTask(task, isManual);
    }

    /**
     * Used by Controller for manual trigger
     */
    public void executeTask(String taskId) {
        NotificationTask task = notificationTaskMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NotificationTask>()
                .eq(NotificationTask::getTaskId, taskId)
        );
        if (Objects.isNull(task)) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        taskCoreProcessor.executeTask(task, true);
    }

    // Helper method exposed for Schedulers if they need it directly?
    // DbPollingScheduler currently calls taskExecutionService.updateNextRunAt.
    // So we should delegate that too or keep it here?
    // TaskCoreProcessor has 'updateNextRunAt'. We can expose it via facade.
    public void updateNextRunAt(NotificationTask task, java.time.LocalDateTime now) {
        taskCoreProcessor.updateNextRunAt(task, now);
    }
}
