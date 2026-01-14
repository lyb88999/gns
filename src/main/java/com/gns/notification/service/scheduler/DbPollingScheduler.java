package com.gns.notification.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@ConditionalOnProperty(name = "app.scheduler.type", havingValue = "db", matchIfMissing = true)
public class DbPollingScheduler implements TaskSchedulerEngine {

    private final NotificationTaskMapper notificationTaskMapper;
    private final TaskExecutionService taskExecutionService;

    public DbPollingScheduler(NotificationTaskMapper notificationTaskMapper, TaskExecutionService taskExecutionService) {
        this.notificationTaskMapper = notificationTaskMapper;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void schedule(NotificationTask task) {
        // DB Polling picks up tasks automatically from DB, so no explicit action needed here
        // unless we want to calculate nextRunAt immediately if missing.
        if (Objects.isNull(task.getNextRunAt())) {
            taskExecutionService.updateNextRunAt(task, LocalDateTime.now());
        }
    }

    @Override
    public void remove(NotificationTask task) {
        // No action needed for DB polling
    }

    @Scheduled(fixedDelay = 5000) // Check every 5 seconds
    public void scheduleTasks() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Initialize tasks with no nextRunAt
        List<NotificationTask> newTasks = notificationTaskMapper.selectList(new LambdaQueryWrapper<NotificationTask>()
                .eq(NotificationTask::getStatus, true)
                .eq(NotificationTask::getTriggerType, "cron")
                .isNull(NotificationTask::getNextRunAt));

        for (NotificationTask task : newTasks) {
            taskExecutionService.updateNextRunAt(task, now);
        }

        // 2. Find tasks due for execution
        List<NotificationTask> dueTasks = notificationTaskMapper.selectList(new LambdaQueryWrapper<NotificationTask>()
                .eq(NotificationTask::getStatus, true)
                .eq(NotificationTask::getTriggerType, "cron")
                .le(NotificationTask::getNextRunAt, now));

        for (NotificationTask task : dueTasks) {
            taskExecutionService.executeTask(task, false);
        }
    }
}
