package com.pulsewatch.backend.monitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_activities")
public class WorkspaceActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "triggered_by", nullable = false)
    private UUID triggeredBy;

    @Column(name = "action_type", nullable = false)
    private String actionType; // CREATE_MONITOR, UPDATE_MONITOR, PAUSE_MONITOR, MERGE_DRAFT, INCIDENT_EVENT

    @Column(nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public WorkspaceActivity() {}

    public WorkspaceActivity(UUID workspaceId, UUID triggeredBy, String actionType, String description) {
        this.workspaceId = workspaceId;
        this.triggeredBy = triggeredBy;
        this.actionType = actionType;
        this.description = description;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public UUID getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(UUID triggeredBy) { this.triggeredBy = triggeredBy; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
