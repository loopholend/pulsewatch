package com.pulsewatch.backend.alert.dto;

import java.util.UUID;

public class AlertRuleRequest {
    private UUID monitorId;
    private String ruleType; // INCIDENT_OPENED, INCIDENT_RESOLVED
    private String notificationEmail;
    private Integer cooldownMinutes;
    private String layoutType = "DEFAULT";
    private String customSubject;
    private String customBody;

    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getNotificationEmail() { return notificationEmail; }
    public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public String getLayoutType() { return layoutType; }
    public void setLayoutType(String layoutType) { this.layoutType = layoutType; }
    public String getCustomSubject() { return customSubject; }
    public void setCustomSubject(String customSubject) { this.customSubject = customSubject; }
    public String getCustomBody() { return customBody; }
    public void setCustomBody(String customBody) { this.customBody = customBody; }
}
