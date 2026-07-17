package com.pulsewatch.backend.auth.dto;

import java.util.UUID;

public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";
    private String email;
    private String role;
    private UUID workspaceId;
    private String workspaceRole;

    public JwtResponse(String accessToken, String refreshToken, String email, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.role = role;
    }

    public JwtResponse(String accessToken, String refreshToken, String email, String role, UUID workspaceId, String workspaceRole) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.role = role;
        this.workspaceId = workspaceId;
        this.workspaceRole = workspaceRole;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public String getWorkspaceRole() { return workspaceRole; }
    public void setWorkspaceRole(String workspaceRole) { this.workspaceRole = workspaceRole; }
}
