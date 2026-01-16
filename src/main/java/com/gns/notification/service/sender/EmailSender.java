package com.gns.notification.service.sender;

import java.util.List;

public interface EmailSender {

    void send(String to, String subject, String content, boolean html, List<EmailAttachment> attachments);
}
