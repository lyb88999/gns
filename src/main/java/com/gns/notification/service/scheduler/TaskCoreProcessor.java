package com.gns.notification.service.scheduler;

import com.gns.notification.domain.NotificationLog;
import com.gns.notification.domain.NotificationLogMapper;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import com.gns.notification.dto.NotificationSendRequest;
import com.gns.notification.exception.RateLimitExceededException;
import com.gns.notification.security.UserContext;
import com.gns.notification.security.UserContextHolder;
import com.gns.notification.service.NotificationSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class TaskCoreProcessor {

    private final NotificationTaskMapper notificationTaskMapper;
    private final NotificationSendService notificationSendService;
    private final NotificationLogMapper notificationLogMapper;

    public TaskCoreProcessor(NotificationTaskMapper notificationTaskMapper,
                             NotificationSendService notificationSendService,
                             NotificationLogMapper notificationLogMapper) {
        this.notificationTaskMapper = notificationTaskMapper;
        this.notificationSendService = notificationSendService;
        this.notificationLogMapper = notificationLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeTask(NotificationTask task, boolean isManual) {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            UserContextHolder.set(
                new UserContext(task.getUserId(), task.getTeamId(), "user")
            );

            NotificationSendRequest request = new NotificationSendRequest();
            request.setTaskId(task.getTaskId());
            request.setData(Objects.nonNull(task.getCustomData()) ? task.getCustomData() : Collections.emptyMap());
            notificationSendService.send(request);

            task.setLastRunAt(now);
            updateNextRunAt(task, now);
        } catch (Exception e) {
            handleExecutionError(task, e, now);
            if (isManual) {
                throw (RuntimeException) e;
            }
        } finally {
            UserContextHolder.clear();
        }
    }

    private void handleExecutionError(NotificationTask task, Exception e, LocalDateTime now) {
        log.error("Failed to execute task: {}", task.getTaskId(), e);
        NotificationLog log = new NotificationLog();
        log.setNotificationId(UUID.randomUUID().toString());
        log.setTaskId(task.getTaskId());
        log.setTaskName(task.getName());
        log.setUserId(task.getUserId());
        log.setChannel("System");
        log.setRecipient("N/A");

        if (e instanceof RateLimitExceededException) {
             if (Objects.nonNull(e.getMessage()) && e.getMessage().contains("Silent Mode")) {
                 log.setStatus("skipped");
             } else {
                 log.setStatus("blocked");
             }
         } else {
             log.setStatus("failed");
         }
        log.setErrorMessage(e.getMessage());
        log.setSentAt(now);
        log.setCreatedAt(now);
        notificationLogMapper.insert(log);

        updateNextRunAt(task, now);
    }

    public void updateNextRunAt(NotificationTask task, LocalDateTime now) {
        try {
            CronExpression cron = CronExpression.parse(task.getCronExpression());
            LocalDateTime next = cron.next(now);
            task.setNextRunAt(next);
            notificationTaskMapper.updateById(task);
        } catch (Exception e) {
            System.err.println("Invalid cron for task: " + task.getTaskId());
        }
    }
}
