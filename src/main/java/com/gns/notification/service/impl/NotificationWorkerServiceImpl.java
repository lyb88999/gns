package com.gns.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.NotificationTaskMapper;
import com.gns.notification.domain.User;
import com.gns.notification.domain.UserMapper;
import com.gns.notification.service.EmailAttachment;
import com.gns.notification.service.EmailSender;
import com.gns.notification.service.NotificationWorkerService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String streamKey;
    private final String consumerGroup;
    private final String consumerName;

    private final com.gns.notification.service.DingTalkSender dingTalkSender;
    private final com.gns.notification.domain.NotificationLogMapper tableLogMapper;

    public NotificationWorkerServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                         NotificationTaskMapper taskMapper,
                                         UserMapper userMapper,
                                         EmailSender emailSender,
                                         com.gns.notification.service.DingTalkSender dingTalkSender,
                                         com.gns.notification.domain.NotificationLogMapper tableLogMapper,
                                         @Value("${app.notification.redis-stream-key}") String streamKey,
                                         @Value("${app.notification.consumer-group}") String consumerGroup,
                                         @Value("${app.notification.consumer-name}") String consumerName) {
        this.redisTemplate = redisTemplate;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.emailSender = emailSender;
        this.dingTalkSender = dingTalkSender;
        this.tableLogMapper = tableLogMapper;
        this.streamKey = streamKey;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
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
        if (messages == null || messages.isEmpty()) {
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
        NotificationTask task = taskMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NotificationTask>()
            .eq(NotificationTask::getTaskId, taskId));
        if (task == null) {
            log.warn("[Worker] task not found for record {} taskId={}", recordId, taskId);
            return;
        }
        Long userId = value.get("userId") instanceof Number ? ((Number) value.get("userId")).longValue() : null;
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || user.getEmail() == null) {
            log.warn("[Worker] user or email missing for record {} userId={}", recordId, userId);
            return;
        }

        Map<String, Object> data = safeMap(value.get("data"));
        String subject = data.getOrDefault("title", task.getName()) == null
            ? task.getName()
            : String.valueOf(data.getOrDefault("title", task.getName()));
        String rendered = renderTemplate(task.getMessageTemplate(), data);

        // 1. Email (Legacy)
        if (task.getChannels() != null && task.getChannels().contains("Email")) {
             boolean html = rendered.contains("<html") || rendered.contains("<body");
             List<String> receivers = resolveReceivers(data, user.getEmail());
             List<EmailAttachment> attachments = parseAttachments(value.get("attachments"));

             for (String receiver : receivers) {
                 try {
                     emailSender.send(receiver, subject, rendered, html, attachments);
                     saveLog(task, "Email", receiver, com.gns.notification.enums.NotificationStatus.SUCCESS.getValue(), null);
                 } catch (Exception e) {
                     e.printStackTrace();
                     saveLog(task, "Email", receiver, com.gns.notification.enums.NotificationStatus.FAILED.getValue(), e.getMessage());
                 }
             }
        }

        // 2. DingTalk
        if (task.getChannels() != null && task.getChannels().contains("DingTalk")) {
             dispatchDingTalk(task, rendered);
        }
    }

    private void dispatchDingTalk(NotificationTask task, String content) {
        if (task.getCustomData() == null) {
            return;
        }
        String webhook = (String) task.getCustomData().get("dingTalkWebhook");
        String secret = (String) task.getCustomData().get("dingTalkSecret");
        if (webhook != null) {
            try {
                dingTalkSender.send(webhook, secret, content);
                saveLog(task, "DingTalk", webhook, com.gns.notification.enums.NotificationStatus.SUCCESS.getValue(), null);
            } catch (Exception e) {
                e.printStackTrace(); // Log error for debugging
                saveLog(task, "DingTalk", webhook, com.gns.notification.enums.NotificationStatus.FAILED.getValue(), e.getMessage());
            }
        }
    }

    private void saveLog(NotificationTask task, String channel, String recipient, String status, String error) {
        com.gns.notification.domain.NotificationLog log = new com.gns.notification.domain.NotificationLog();
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
        return java.util.Collections.emptyMap();
    }

    private String renderTemplate(String template, Map<String, Object> data) throws JsonProcessingException {
        if (template == null) {
            return objectMapper.writeValueAsString(data);
        }
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = data.get(key);
            String replacement = value == null ? "" : value.toString();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<String> resolveReceivers(Map<String, Object> data, String fallbackEmail) {
        List<String> receivers = new ArrayList<>();
        Object rcObj = data.get("receivers");
        if (rcObj instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    receivers.add(String.valueOf(item));
                }
            }
        }
        if (receivers.isEmpty() && fallbackEmail != null) {
            receivers.add(fallbackEmail);
        }
        return receivers;
    }

    private List<EmailAttachment> parseAttachments(Object attachmentsObj) {
        List<EmailAttachment> attachments = new ArrayList<>();
        if (!(attachmentsObj instanceof List<?> list)) {
            return attachments;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String filename = Objects.toString(map.get("filename"), null);
            Object contentObj = map.get("content");
            if (contentObj == null) {
                continue;
            }
            try {
                byte[] bytes = Base64.getDecoder().decode(String.valueOf(contentObj));
                attachments.add(new EmailAttachment(filename, bytes));
            } catch (IllegalArgumentException e) {
                log.warn("Skip invalid attachment base64 for file {}", filename);
            }
        }
        return attachments;
    }
}
