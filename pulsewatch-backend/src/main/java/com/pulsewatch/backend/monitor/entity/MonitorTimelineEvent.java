package com.pulsewatch.backend.monitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monitor_timeline_events")
public class MonitorTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "monitor_id", nullable = false)
    private UUID monitorId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "event_type", nullable = false)
    private String eventType; // CREATED, CONFIG_CHANGED, INCIDENT_OPENED, INCIDENT_RESOLVED, DRAFT_MERGED, PAUSED, RESUMED

    @Column(nullable = false)
    private String message;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MonitorTimelineEvent() {}

    public MonitorTimelineEvent(UUID monitorId, UUID workspaceId, String eventType, String message, UUID triggeredBy) {
        this.monitorId = monitorId;
        this.workspaceId = workspaceId;
        this.eventType = eventType;
        this.message = message;
        this.triggeredBy = triggeredBy;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UUID getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(UUID triggeredBy) { this.triggeredBy = triggeredBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
