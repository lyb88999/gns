package com.gns.sdk.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendRequest {
    private String taskId;
    private Map<String, Object> data;
    private List<Attachment> attachments;
    private String priority;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String filename;
        private String content; // base64
    }
}
