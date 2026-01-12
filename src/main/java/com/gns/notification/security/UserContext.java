package com.gns.notification.security;

public class UserContext {

    private final Long userId;
    private final Long teamId;
    private final String role;

    public UserContext(Long userId, Long teamId, String role) {
        this.userId = userId;
        this.teamId = teamId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean isTeamAdmin() {
        return isAdmin() || "team_admin".equalsIgnoreCase(role);
    }
}
