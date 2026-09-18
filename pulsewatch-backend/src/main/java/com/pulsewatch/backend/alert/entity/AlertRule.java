package com.pulsewatch.backend.alert.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The workspace this alert rule belongs to.
     * Derived from the monitor's workspace at creation time.
     * Invariant: alertRule.workspaceId == monitor.workspaceId (enforced in AlertService).
     */
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "monitor_id", nullable = false)
    private UUID monitorId;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType; // INCIDENT_OPENED, INCIDENT_RESOLVED

    @Column(name = "notification_email", nullable = false, length = 255)
    private String notificationEmail;

    @Column(name = "cooldown_minutes", nullable = false)
    private Integer cooldownMinutes = 30;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_alert_sent_at")
    private LocalDateTime lastAlertSentAt;

    @Column(name = "layout_type", nullable = false, length = 50)
    private String layoutType = "DEFAULT";

    @Column(name = "custom_subject", length = 255)
    private String customSubject;

    @Column(name = "custom_body", columnDefinition = "TEXT")
    private String customBody;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AlertRule() {}

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getNotificationEmail() { return notificationEmail; }
    public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getLastAlertSentAt() { return lastAlertSentAt; }
    public void setLastAlertSentAt(LocalDateTime lastAlertSentAt) { this.lastAlertSentAt = lastAlertSentAt; }
    public String getLayoutType() { return layoutType; }
    public void setLayoutType(String layoutType) { this.layoutType = layoutType; }
    public String getCustomSubject() { return customSubject; }
    public void setCustomSubject(String customSubject) { this.customSubject = customSubject; }
    public String getCustomBody() { return customBody; }
    public void setCustomBody(String customBody) { this.customBody = customBody; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
