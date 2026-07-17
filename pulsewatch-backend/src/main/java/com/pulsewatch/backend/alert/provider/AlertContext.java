package com.pulsewatch.backend.alert.provider;

import java.util.UUID;

/**
 * Context object passed to AlertProvider when an alert needs to be sent.
 */
public class AlertContext {
    private UUID monitorId;
    private String monitorName;
    private String alertType;       // INCIDENT_OPENED, INCIDENT_RESOLVED
    private String severity;
    private String recipient;
    private UUID incidentId;
    private Long durationSeconds;   // only set for INCIDENT_RESOLVED
    private String layoutType = "DEFAULT";
    private String customSubject;
    private String customBody;

    public AlertContext() {}

    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public String getMonitorName() { return monitorName; }
    public void setMonitorName(String monitorName) { this.monitorName = monitorName; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getLayoutType() { return layoutType; }
    public void setLayoutType(String layoutType) { this.layoutType = layoutType; }
    public String getCustomSubject() { return customSubject; }
    public void setCustomSubject(String customSubject) { this.customSubject = customSubject; }
    public String getCustomBody() { return customBody; }
    public void setCustomBody(String customBody) { this.customBody = customBody; }
}
