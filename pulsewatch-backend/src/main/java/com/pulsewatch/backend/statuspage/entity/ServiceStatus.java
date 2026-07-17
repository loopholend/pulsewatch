package com.pulsewatch.backend.statuspage.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_status",
       uniqueConstraints = @UniqueConstraint(columnNames = {"status_page_id", "monitor_id"}))
public class ServiceStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "status_page_id", nullable = false)
    private UUID statusPageId;

    @Column(name = "monitor_id", nullable = false)
    private UUID monitorId;

    @Column(name = "service_name", length = 255)
    private String serviceName;

    @Column(name = "current_status", nullable = false, length = 50)
    private String currentStatus; // UP, DOWN, UNKNOWN

    @Column(name = "last_checked", nullable = false)
    private LocalDateTime lastChecked;

    public ServiceStatus() {}

    public UUID getId() { return id; }
    public UUID getStatusPageId() { return statusPageId; }
    public void setStatusPageId(UUID statusPageId) { this.statusPageId = statusPageId; }
    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public LocalDateTime getLastChecked() { return lastChecked; }
    public void setLastChecked(LocalDateTime lastChecked) { this.lastChecked = lastChecked; }
}
