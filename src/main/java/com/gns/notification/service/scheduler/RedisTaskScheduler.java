package com.gns.notification.service.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "app.scheduler.type", havingValue = "redis")
@Slf4j
public class RedisTaskScheduler implements TaskSchedulerEngine {

    private static final String SCHEDULER_KEY = "gns:scheduler:tasks";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationTaskMapper notificationTaskMapper;
    private final TaskExecutionService taskExecutionService;

    public RedisTaskScheduler(RedisTemplate<String, Object> redisTemplate,
                              NotificationTaskMapper notificationTaskMapper,
                              TaskExecutionService taskExecutionService) {
        this.redisTemplate = redisTemplate;
        this.notificationTaskMapper = notificationTaskMapper;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void schedule(NotificationTask task) {
        if (!Boolean.TRUE.equals(task.getStatus()) || !"cron".equals(task.getTriggerType())) {
            remove(task);
            return;
        }
        
        // Calculate next run
        try {
            CronExpression cron = CronExpression.parse(task.getCronExpression());
            // Fix: Use Asia/Shanghai time for scheduling
            ZoneId zoneId = ZoneId.of("Asia/Shanghai");
            LocalDateTime now = LocalDateTime.now(zoneId);
            LocalDateTime next = cron.next(now);
            
            if (Objects.nonNull(next)) {
                // Determine score (epoch milli) using specific zone offset
                double score = next.toInstant(zoneId.getRules().getOffset(next)).toEpochMilli();
                
                redisTemplate.opsForZSet().add(SCHEDULER_KEY, task.getTaskId(), score);
                
                // Also update DB for UI display
                task.setNextRunAt(next);
                notificationTaskMapper.updateById(task);
            }
        } catch (Exception e) {
            log.error("Invalid cron for task: {}", task.getTaskId());
        }
    }

    @Override
    public void remove(NotificationTask task) {
        redisTemplate.opsForZSet().remove(SCHEDULER_KEY, task.getTaskId());
    }

    @Scheduled(fixedDelay = 1000) // Poll Redis every second
    public void start() {
        long now = System.currentTimeMillis();
        // Get tasks due (0 to now)
        Set<Object> taskIds = redisTemplate.opsForZSet().rangeByScore(SCHEDULER_KEY, 0, now);
        
        if (Objects.nonNull(taskIds) && !taskIds.isEmpty()) {
            for (Object obj : taskIds) {
                String taskId = (String) obj;
                
                // Prevent double execution by other nodes: Check and Remove (simplistic lock)
                // In production, we might use Lua script to atomic remove-and-return
                // For now, simpler: remove. If result > 0, we own it.
                Long removed = redisTemplate.opsForZSet().remove(SCHEDULER_KEY, taskId);
                if (Objects.nonNull(removed) && removed > 0) {
                     // We got the task
                     // Fetch full task details
                     NotificationTask task = notificationTaskMapper.selectOne(
                         new LambdaQueryWrapper<NotificationTask>()
                             .eq(NotificationTask::getTaskId, taskId)
                     );
                     
                     if (Objects.nonNull(task) && Boolean.TRUE.equals(task.getStatus())) {
                         try {
                             taskExecutionService.executeTask(task, false);
                         } catch (Exception e) {
                             log.error("Task execution failed (scheduler safe-guard): {}", e.getMessage());
                         } finally {
                             // Always reschedule if task is still active
                             schedule(task);
                         }
                     } else {
                         // Task deleted or disabled, do not reschedule
                     }
                }
            }
        }
    }
    
    @PostConstruct
    public void init() {
        // Load all active cron tasks from DB to Redis on startup
        // This ensures pending tasks are picked up after restart or mode switch
        java.util.List<NotificationTask> tasks = notificationTaskMapper.selectList(
            new LambdaQueryWrapper<NotificationTask>()
                .eq(NotificationTask::getStatus, true)
                .eq(NotificationTask::getTriggerType, "cron")
        );
        
        if (tasks != null) {
            for (NotificationTask task : tasks) {
                schedule(task);
            }
            System.out.println("Initialized Redis Scheduler with " + tasks.size() + " tasks.");
        }
    }
}
