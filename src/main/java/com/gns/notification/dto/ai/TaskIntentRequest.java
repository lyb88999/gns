package com.gns.notification.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskIntentRequest {
    @NotBlank(message = "Prompt cannot be empty")
    private String prompt;
}
