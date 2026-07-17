package com.pulsewatch.backend.incident.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class IncidentResponse {
    private UUID id;
    private UUID monitorId;
    private String status;
    private String severity;
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    private Long durationSeconds;
    private String failureReason;
    private String monitorName;

    public IncidentResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public String getMonitorName() { return monitorName; }
    public void setMonitorName(String monitorName) { this.monitorName = monitorName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
