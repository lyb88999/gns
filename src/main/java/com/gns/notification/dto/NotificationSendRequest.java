package com.gns.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public class NotificationSendRequest {

    @NotBlank
    private String taskId;

    @NotEmpty
    private Map<String, Object> data;

    private List<AttachmentPayload> attachments;

    private String priority;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public List<AttachmentPayload> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentPayload> attachments) {
        this.attachments = attachments;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public static class AttachmentPayload {
        private String filename;
        private String content; // base64

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
