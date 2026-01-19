package com.gns.notification.service.impl;

import com.gns.notification.service.sender.EmailSender;
import com.gns.notification.service.sender.EmailAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Objects;

@Service
public class EmailSenderImpl implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailSenderImpl(JavaMailSender mailSender,
                           @Value("${spring.mail.from:${spring.mail.username}}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String to, String subject, String content, boolean html, List<EmailAttachment> attachments) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            boolean hasAttachments = Objects.nonNull(attachments) && !attachments.isEmpty();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasAttachments, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, html);
            helper.setFrom(fromAddress);
            if (hasAttachments) {
                for (EmailAttachment attachment : attachments) {
                    if (attachment.getContent() == null) {
                        continue;
                    }
                    String filename = attachment.getFilename() == null ? "attachment" : attachment.getFilename();
                    helper.addAttachment(filename, new ByteArrayResource(attachment.getContent()));
                }
            }
            mailSender.send(mimeMessage);
            log.info("Email sent to {} subject={}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
