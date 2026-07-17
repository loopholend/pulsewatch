package com.pulsewatch.backend.statuspage.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public-facing response for a status page — safe to return without authentication.
 */
public class PublicStatusPageResponse {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private String overallStatus; // UP, DEGRADED, DOWN
    private LocalDateTime lastUpdated;
    private boolean hasActiveIncident;
    private List<ServiceStatusItem> services;

    public static class ServiceStatusItem {
        private UUID monitorId;
        private String serviceName;
        private String currentStatus;
        private LocalDateTime lastChecked;
        private Double uptimePercent;    // 30-day rolling uptime %
        private Integer avgResponseTimeMs; // recent average latency

        public UUID getMonitorId() { return monitorId; }
        public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
        public LocalDateTime getLastChecked() { return lastChecked; }
        public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }
        public Double getUptimePercent() { return uptimePercent; }
        public void setUptimePercent(Double uptimePercent) { this.uptimePercent = uptimePercent; }
        public Integer getAvgResponseTimeMs() { return avgResponseTimeMs; }
        public void setAvgResponseTimeMs(Integer avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    public boolean isHasActiveIncident() { return hasActiveIncident; }
    public void setHasActiveIncident(boolean hasActiveIncident) { this.hasActiveIncident = hasActiveIncident; }
    public List<ServiceStatusItem> getServices() { return services; }
    public void setServices(List<ServiceStatusItem> services) { this.services = services; }
}
