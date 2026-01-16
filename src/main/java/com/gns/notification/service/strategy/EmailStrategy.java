package com.gns.notification.service.strategy;

import com.gns.notification.domain.NotificationTask;
import com.gns.notification.domain.User;
import com.gns.notification.service.sender.EmailAttachment;
import com.gns.notification.service.sender.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class EmailStrategy implements NotificationChannelStrategy {

    private final EmailSender emailSender;

    public EmailStrategy(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public String getChannelName() {
        return "Email";
    }

    @Override
    public List<DispatchResult> send(NotificationTask task, String content, User user, Map<String, Object> data) {
        List<DispatchResult> results = new ArrayList<>();
        boolean html = content.contains("<html") || content.contains("<body");
        List<String> receivers = resolveReceivers(data, user.getEmail());
        List<EmailAttachment> attachments = parseAttachments(data.get("attachments"));

        for (String receiver : receivers) {
            try {
                emailSender.send(receiver, task.getName(), content, html, attachments);
                log.info("Sent email to {}", receiver);
                results.add(DispatchResult.success(receiver));
            } catch (Exception e) {
                log.error("Failed to send email to {}", receiver, e);
                results.add(DispatchResult.failure(receiver, e.getMessage()));
            }
        }
        return results;
    }

    private List<String> resolveReceivers(Map<String, Object> data, String fallbackEmail) {
        List<String> receivers = new ArrayList<>();
        Object rcObj = data.get("receivers");
        if (rcObj instanceof List<?> list) {
            for (Object item : list) {
                if (Objects.nonNull(item)) {
                    receivers.add(String.valueOf(item));
                }
            }
        }
        if (receivers.isEmpty() && Objects.nonNull(fallbackEmail)) {
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
            if (Objects.isNull(contentObj)) {
                continue;
            }
            try {
                byte[] bytes = Base64.getDecoder().decode(String.valueOf(contentObj));
                attachments.add(new EmailAttachment(filename, bytes));
            } catch (IllegalArgumentException e) {
                log.warn("Failed to decode attachment content");
            }
        }
        return attachments;
    }
}
