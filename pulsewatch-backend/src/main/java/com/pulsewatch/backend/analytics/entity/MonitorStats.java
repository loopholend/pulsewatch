package com.pulsewatch.backend.analytics.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monitor_stats")
public class MonitorStats {

    @Id
    @Column(name = "monitor_id")
    private UUID monitorId;

    @Column
    private Double uptime = 0.0;

    @Column(name = "avg_latency")
    private Integer avgLatency = 0;

    @Column(name = "p95_latency")
    private Integer p95Latency = 0;

    @Column(name = "error_rate")
    private Double errorRate = 0.0;

    @Column(name = "total_checks")
    private Long totalChecks = 0L;

    @Column(name = "successful_checks")
    private Long successfulChecks = 0L;

    @Column(name = "failed_checks")
    private Long failedChecks = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MonitorStats() {}

    public MonitorStats(UUID monitorId) {
        this.monitorId = monitorId;
    }

    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public Double getUptime() { return uptime; }
    public void setUptime(Double uptime) { this.uptime = uptime; }
    public Integer getAvgLatency() { return avgLatency; }
    public void setAvgLatency(Integer avgLatency) { this.avgLatency = avgLatency; }
    public Integer getP95Latency() { return p95Latency; }
    public void setP95Latency(Integer p95Latency) { this.p95Latency = p95Latency; }
    public Double getErrorRate() { return errorRate; }
    public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
    public Long getTotalChecks() { return totalChecks; }
    public void setTotalChecks(Long totalChecks) { this.totalChecks = totalChecks; }
    public Long getSuccessfulChecks() { return successfulChecks; }
    public void setSuccessfulChecks(Long successfulChecks) { this.successfulChecks = successfulChecks; }
    public Long getFailedChecks() { return failedChecks; }
    public void setFailedChecks(Long failedChecks) { this.failedChecks = failedChecks; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
