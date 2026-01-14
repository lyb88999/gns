package com.gns.notification;

import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import com.gns.notification.exception.RateLimitExceededException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TaskIntegrationTest {

    @Autowired
    private com.gns.notification.service.scheduler.TaskExecutionService taskExecutionService;

    @Autowired
    private NotificationTaskMapper taskMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
    }

    @AfterEach
    void tearDown() {
        // Cleanup DB
        taskMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NotificationTask>()
                .eq(NotificationTask::getTaskId, taskId));
        
        // Cleanup Redis
        Set<String> keys = redisTemplate.keys("gns:limit:task:" + taskId + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private NotificationTask createTask(boolean rateLimit, Integer maxHour, LocalTime silentStart, LocalTime silentEnd) {
        NotificationTask task = new NotificationTask();
        task.setTaskId(taskId);
        task.setName("Integration Test Task");
        task.setUserId(1L); // Assuming user 1 exists or validation skipped for test
        task.setTeamId(1L);
        task.setChannels(Collections.singletonList("Email"));
        task.setTriggerType("cron");
        task.setCronExpression("0 0 12 * * ?");
        task.setMessageTemplate("Test Template");
        task.setStatus(true);
        task.setRateLimitEnabled(rateLimit);
        task.setMaxPerHour(maxHour);
        task.setSilentStart(silentStart);
        task.setSilentEnd(silentEnd);
        taskMapper.insert(task);
        return task;
    }

    @Test
    void testManualTriggerSuccess() {
        createTask(false, null, null, null);
        
        assertDoesNotThrow(() -> taskExecutionService.executeTask(taskId));
    }

    @Test
    void testRateLimitBlock() {
        createTask(true, 1, null, null);

        // First run: Success
        assertDoesNotThrow(() -> taskExecutionService.executeTask(taskId));

        // Second run: Should fail
        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class, () -> {
            taskExecutionService.executeTask(taskId);
        });
        
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    void testSilentModeBlock() {
        // Set silent mode to cover current time
        LocalTime now = LocalTime.now();
        LocalTime start = now.minusMinutes(30);
        LocalTime end = now.plusMinutes(30);
        
        createTask(false, null, start, end);

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class, () -> {
            taskExecutionService.executeTask(taskId);
        });

        assertTrue(exception.getMessage().contains("Silent Mode is active"));
    }
}
