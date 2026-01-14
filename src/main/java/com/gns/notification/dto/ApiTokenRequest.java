package com.gns.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class ApiTokenRequest {

    @NotBlank
    private String name;

    private List<String> scopes;

    private LocalDateTime expiresAt;

}
