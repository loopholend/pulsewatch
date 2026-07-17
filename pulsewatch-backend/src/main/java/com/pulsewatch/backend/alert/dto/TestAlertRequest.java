package com.pulsewatch.backend.alert.dto;

import java.util.UUID;

public class TestAlertRequest {
    private UUID monitorId;
    private String email;

    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
