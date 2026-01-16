package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gns.notification.domain.*;
import com.gns.notification.enums.NotificationStatus;
import com.gns.notification.service.NotificationWorkerService;
import com.gns.notification.service.strategy.DispatchResult;
import com.gns.notification.service.strategy.NotificationChannelStrategy;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class NotificationWorkerServiceImpl implements NotificationWorkerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorkerServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationTaskMapper taskMapper;
    private final UserMapper userMapper;
    private final NotificationLogMapper tableLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String streamKey;
    private final String consumerGroup;
    private final String consumerName;

    private final Map<String, NotificationChannelStrategy> strategyMap;

    public NotificationWorkerServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                         NotificationTaskMapper taskMapper,
                                         UserMapper userMapper,
                                         NotificationLogMapper tableLogMapper,
                                         List<NotificationChannelStrategy> strategies,
                                         @Value("${app.notification.redis-stream-key}") String streamKey,
                                         @Value("${app.notification.consumer-group}") String consumerGroup,
                                         @Value("${app.notification.consumer-name}") String consumerName) {
        this.redisTemplate = redisTemplate;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.tableLogMapper = tableLogMapper;
        this.streamKey = streamKey;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(NotificationChannelStrategy::getChannelName, Function.identity()));
    }

    @PostConstruct
    public void initGroup() {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, consumerGroup);
            log.info("Created Redis Stream group {} for key {}", consumerGroup, streamKey);
        } catch (Exception e) {
            log.info("Redis Stream group may already exist: {} -> {}", streamKey, consumerGroup);
        }
    }

    @Override
    @Scheduled(fixedDelayString = "PT5S")
    public void pollAndSend() {
        StreamReadOptions options = StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1));
        StreamOffset<String> offset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());
        List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream()
            .read(Consumer.from(consumerGroup, consumerName), options, offset);
        if (Objects.isNull(messages) || messages.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : messages) {
            Map<Object, Object> value = record.getValue();
            try {
                handleRecord(record.getId().toString(), value);
                redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
            } catch (Exception e) {
                log.error("[Worker] failed to process record id={} err={}", record.getId(), e.getMessage(), e);
            }
        }
    }

    private void handleRecord(String recordId, Map<Object, Object> value) throws JsonProcessingException {
        String taskId = (String) value.get("taskId");
        NotificationTask task = taskMapper.selectOne(new LambdaQueryWrapper<NotificationTask>()
            .eq(NotificationTask::getTaskId, taskId));
        if (Objects.isNull(task)) {
            log.warn("[Worker] task not found for record {} taskId={}", recordId, taskId);
            return;
        }
        Long userId = value.get("userId") instanceof Number ? ((Number) value.get("userId")).longValue() : null;
        User user = Objects.isNull(userId) ? null : userMapper.selectById(userId);
        if (Objects.isNull(user)) {
             // Create a dummy user context if missing, or log warn. 
             // Strategies might not strictly need User entity if receivers are in payload.
             // But existing logic checked user.getEmail().
             if (Objects.nonNull(userId)) {
                 log.warn("[Worker] user not found id={}", userId);
             }
             user = new User(); // Avoid NPE in strategies
        }

        log.info("[Worker] Processing recordId={} taskId={}", recordId, taskId);

        Map<String, Object> data = safeMap(value.get("data"));
        // Inject attachments into data so EmailStrategy can find it
        if (value.containsKey("attachments")) {
            Object attachments = value.get("attachments");
            if (attachments instanceof String) {
                try {
                    attachments = objectMapper.readValue((String) attachments, List.class);
                } catch (Exception e) {
                   log.warn("[Worker] failed to parse attachments JSON", e);
                }
            }
            data.put("attachments", attachments);
        }
        
        String rendered = renderTemplate(task.getMessageTemplate(), data);

        if (Objects.nonNull(task.getChannels())) {
            for (String channel : task.getChannels()) {
                NotificationChannelStrategy strategy = strategyMap.get(channel);
                if (Objects.nonNull(strategy)) {
                    List<DispatchResult> results = strategy.send(task, rendered, user, data);
                    log.info("[Worker] Dispatched to channel={} count={}", channel, results.size());
                    for (DispatchResult result : results) {
                        saveLog(task, channel, result.getRecipient(), 
                                result.isSuccess() ? NotificationStatus.SUCCESS.getValue() : NotificationStatus.FAILED.getValue(), 
                                result.getErrorMessage());
                    }
                } else {
                    log.warn("No strategy found for channel: {}", channel);
                }
            }
        }
    }

    private void saveLog(NotificationTask task, String channel, String recipient, String status, String error) {
        NotificationLog log = new NotificationLog();
        log.setNotificationId(java.util.UUID.randomUUID().toString());
        log.setTaskId(task.getTaskId());
        log.setTaskName(task.getName());
        log.setChannel(channel);
        log.setRecipient(recipient);
        log.setStatus(status);
        log.setErrorMessage(error);
        log.setUserId(task.getUserId());
        log.setSentAt(java.time.LocalDateTime.now());
        log.setCreatedAt(java.time.LocalDateTime.now());
        tableLogMapper.insert(log);
    }

    private Map<String, Object> safeMap(Object obj) {
        if (obj instanceof Map<?, ?> mapObj) {
            return mapObj.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    e -> String.valueOf(e.getKey()),
                    Map.Entry::getValue
                ));
        }
        if (obj instanceof String) {
            try {
                return objectMapper.readValue((String) obj, Map.class);
            } catch (Exception e) {
                log.warn("[Worker] failed to parse data JSON: {}", obj, e);
            }
        }
        return new java.util.HashMap<>();
    }

    private String renderTemplate(String template, Map<String, Object> data) throws JsonProcessingException {
        if (Objects.isNull(template)) {
            return objectMapper.writeValueAsString(data);
        }
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = data.get(key);
            String replacement = Objects.isNull(value) ? "" : value.toString();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
