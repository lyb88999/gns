package com.gns.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class NotificationSendRequest {

    @NotBlank
    private String taskId;

    @NotEmpty
    private Map<String, Object> data;

    private List<AttachmentPayload> attachments;

    private String priority;

    @Setter
    @Getter
    public static class AttachmentPayload {
        private String filename;
        private String content; // base64

    }
}
