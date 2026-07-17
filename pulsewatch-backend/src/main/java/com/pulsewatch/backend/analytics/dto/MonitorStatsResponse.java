package com.pulsewatch.backend.analytics.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class MonitorStatsResponse {
    private UUID monitorId;
    private String monitorName;
    private String currentStatus;
    private double uptime;
    private int avgLatency;
    private int p95Latency;
    private double errorRate;
    private long totalChecks;
    private long successChecks;
    private LocalDateTime updatedAt;

    public MonitorStatsResponse() {}

    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public String getMonitorName() { return monitorName; }
    public void setMonitorName(String monitorName) { this.monitorName = monitorName; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public double getUptime() { return uptime; }
    public void setUptime(double uptime) { this.uptime = uptime; }
    public int getAvgLatency() { return avgLatency; }
    public void setAvgLatency(int avgLatency) { this.avgLatency = avgLatency; }
    public int getP95Latency() { return p95Latency; }
    public void setP95Latency(int p95Latency) { this.p95Latency = p95Latency; }
    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    public long getTotalChecks() { return totalChecks; }
    public void setTotalChecks(long totalChecks) { this.totalChecks = totalChecks; }
    public long getSuccessChecks() { return successChecks; }
    public void setSuccessChecks(long successChecks) { this.successChecks = successChecks; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
