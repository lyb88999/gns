package com.gns.notification.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class ApiTokenResponse {

    private Long id;
    private Long userId;
    private String name;
    private List<String> scopes;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String token;

}
