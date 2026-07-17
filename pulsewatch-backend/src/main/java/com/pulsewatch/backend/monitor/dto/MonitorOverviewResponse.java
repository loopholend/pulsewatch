package com.pulsewatch.backend.monitor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class MonitorOverviewResponse {
    private UUID id;
    private String name;
    private String url;
    private String monitorType;
    private String method;
    private int intervalSeconds;
    private int timeoutMs;
    private int expectedStatus;
    private String currentStatus;
    private boolean active;

    // Stats
    private double uptime;
    private int avgLatency;
    private int p95Latency;
    private long totalChecks;
    private long successfulChecks;
    private long failedChecks;

    // Latest check details
    private Integer lastStatusCode;
    private Integer lastLatencyMs;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime nextCheckAt;
    private String lastFailureReason;
    private LocalDateTime lastSuccessfulCheckAt;
    private Integer lastSuccessfulLatencyMs;
    private Integer consecutiveFailures;

    public String getLastFailureReason() { return lastFailureReason; }
    public void setLastFailureReason(String lastFailureReason) { this.lastFailureReason = lastFailureReason; }
    public LocalDateTime getLastSuccessfulCheckAt() { return lastSuccessfulCheckAt; }
    public void setLastSuccessfulCheckAt(LocalDateTime lastSuccessfulCheckAt) { this.lastSuccessfulCheckAt = lastSuccessfulCheckAt; }
    public Integer getLastSuccessfulLatencyMs() { return lastSuccessfulLatencyMs; }
    public void setLastSuccessfulLatencyMs(Integer lastSuccessfulLatencyMs) { this.lastSuccessfulLatencyMs = lastSuccessfulLatencyMs; }
    public Integer getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }

    // Incident
    private String currentIncidentStatus;
    private String currentIncidentSeverity;
    private LocalDateTime currentIncidentStartedAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMonitorType() { return monitorType; }
    public void setMonitorType(String monitorType) { this.monitorType = monitorType; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(int expectedStatus) { this.expectedStatus = expectedStatus; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double getUptime() { return uptime; }
    public void setUptime(double uptime) { this.uptime = uptime; }
    public int getAvgLatency() { return avgLatency; }
    public void setAvgLatency(int avgLatency) { this.avgLatency = avgLatency; }
    public int getP95Latency() { return p95Latency; }
    public void setP95Latency(int p95Latency) { this.p95Latency = p95Latency; }
    public long getTotalChecks() { return totalChecks; }
    public void setTotalChecks(long totalChecks) { this.totalChecks = totalChecks; }
    public long getSuccessfulChecks() { return successfulChecks; }
    public void setSuccessfulChecks(long successfulChecks) { this.successfulChecks = successfulChecks; }
    public long getFailedChecks() { return failedChecks; }
    public void setFailedChecks(long failedChecks) { this.failedChecks = failedChecks; }

    public Integer getLastStatusCode() { return lastStatusCode; }
    public void setLastStatusCode(Integer lastStatusCode) { this.lastStatusCode = lastStatusCode; }
    public Integer getLastLatencyMs() { return lastLatencyMs; }
    public void setLastLatencyMs(Integer lastLatencyMs) { this.lastLatencyMs = lastLatencyMs; }
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    public LocalDateTime getNextCheckAt() { return nextCheckAt; }
    public void setNextCheckAt(LocalDateTime nextCheckAt) { this.nextCheckAt = nextCheckAt; }

    public String getCurrentIncidentStatus() { return currentIncidentStatus; }
    public void setCurrentIncidentStatus(String currentIncidentStatus) { this.currentIncidentStatus = currentIncidentStatus; }
    public String getCurrentIncidentSeverity() { return currentIncidentSeverity; }
    public void setCurrentIncidentSeverity(String currentIncidentSeverity) { this.currentIncidentSeverity = currentIncidentSeverity; }
    public LocalDateTime getCurrentIncidentStartedAt() { return currentIncidentStartedAt; }
    public void setCurrentIncidentStartedAt(LocalDateTime currentIncidentStartedAt) { this.currentIncidentStartedAt = currentIncidentStartedAt; }
}
