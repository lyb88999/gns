package com.gns.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRequest {

    @NotBlank
    @Size(max = 50)
    private String username;

    @Size(min = 6, max = 255)
    private String password;

    @Email
    private String email;

    @NotBlank
    private String role;

    private Long teamId;

    private Integer status = 1;

}
