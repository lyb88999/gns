package com.gns.notification.security;

import lombok.Getter;

@Getter
public class UserContext {

    private final Long userId;
    private final Long teamId;
    private final String role;

    public UserContext(Long userId, Long teamId, String role) {
        this.userId = userId;
        this.teamId = teamId;
        this.role = role;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean isTeamAdmin() {
        return isAdmin() || "team_admin".equalsIgnoreCase(role);
    }
}
