package com.gns.notification.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gns.notification.domain.NotificationLog;
import com.gns.notification.domain.NotificationLogMapper;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import com.gns.notification.dto.NotificationSendRequest;
import com.gns.notification.service.NotificationSendService;
import com.gns.notification.exception.RateLimitExceededException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class TaskSchedulerService {

    private final NotificationTaskMapper notificationTaskMapper;
    private final NotificationSendService notificationSendService;
    private final NotificationLogMapper notificationLogMapper;

    public TaskSchedulerService(NotificationTaskMapper notificationTaskMapper, NotificationSendService notificationSendService, NotificationLogMapper notificationLogMapper) {
        this.notificationTaskMapper = notificationTaskMapper;
        this.notificationSendService = notificationSendService;
        this.notificationLogMapper = notificationLogMapper;
    }

    @Scheduled(fixedDelay = 60000) // Check every minute
    public void scheduleTasks() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Initialize tasks with no nextRunAt
        List<NotificationTask> newTasks = notificationTaskMapper.selectList(new LambdaQueryWrapper<NotificationTask>()
                .eq(NotificationTask::getStatus, true)
                .eq(NotificationTask::getTriggerType, "cron")
                .isNull(NotificationTask::getNextRunAt));

        for (NotificationTask task : newTasks) {
            updateNextRunAt(task, now);
        }

        // 2. Find tasks due for execution
        List<NotificationTask> dueTasks = notificationTaskMapper.selectList(new LambdaQueryWrapper<NotificationTask>()
                .eq(NotificationTask::getStatus, true)
                .eq(NotificationTask::getTriggerType, "cron")
                .le(NotificationTask::getNextRunAt, now));

        for (NotificationTask task : dueTasks) {
            processTask(task, now, false);
        }
    }

    public void processTask(NotificationTask task, LocalDateTime now, boolean throwException) {
        try {
            // Set UserContext for the task execution
            // Role is not strictly needed for sending, but teamId might be.
            // Assuming "user" role for scheduled tasks or fetch from DB if critical.
            com.gns.notification.security.UserContextHolder.set(
                new com.gns.notification.security.UserContext(task.getUserId(), task.getTeamId(), "user")
            );

            // Execute
            NotificationSendRequest request = new NotificationSendRequest();
            request.setTaskId(task.getTaskId());
            request.setData(task.getCustomData() != null ? task.getCustomData() : Collections.emptyMap());
            notificationSendService.send(request);

            // Update next run time
            task.setLastRunAt(now);
            updateNextRunAt(task, now);
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Failed to execute scheduled task: " + task.getId() + ", error: " + e.getMessage());

            // Save failure log
            NotificationLog log = new NotificationLog();
            log.setNotificationId(java.util.UUID.randomUUID().toString());
            log.setTaskId(task.getTaskId());
            log.setTaskName(task.getName());
            log.setUserId(task.getUserId());
            log.setChannel("System"); // Indicates system-level failure/block
            log.setRecipient("N/A");
            
            if (e instanceof RateLimitExceededException) {
                log.setStatus("blocked");
            } else {
                log.setStatus("failed");
            }
            log.setErrorMessage(e.getMessage());
            log.setSentAt(now);
            log.setCreatedAt(now);
            notificationLogMapper.insert(log);
            
            // Avoid infinite loop if cron invalid, push next run to 1 minute later or disable?
            // For now, just try to calc next run from now
             updateNextRunAt(task, now);
             
             if (throwException) {
                 if (e instanceof RuntimeException) {
                     throw (RuntimeException) e;
                 }
                 throw new RuntimeException(e);
             }
        } finally {
            com.gns.notification.security.UserContextHolder.clear();
        }
    }

    private void updateNextRunAt(NotificationTask task, LocalDateTime now) {
        try {
            CronExpression cron = CronExpression.parse(task.getCronExpression());
            LocalDateTime next = cron.next(now);
            task.setNextRunAt(next);
            notificationTaskMapper.updateById(task);
        } catch (Exception e) {
            System.err.println("Invalid cron expression for task: " + task.getId());
            // Optionally disable task
        }
    }

    public void executeTask(String taskId) {
        NotificationTask task = notificationTaskMapper.selectOne(new LambdaQueryWrapper<NotificationTask>()
                .eq(NotificationTask::getTaskId, taskId));
        
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }
        
        // Use current time for manual execution
        processTask(task, LocalDateTime.now(), true);
    }
}
