package com.gns.notification.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private Long teamId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
