package com.pulsewatch.backend.alert.service;

import com.pulsewatch.backend.alert.dto.AlertRuleRequest;
import com.pulsewatch.backend.alert.entity.AlertHistory;
import com.pulsewatch.backend.alert.entity.AlertRule;
import com.pulsewatch.backend.alert.provider.AlertContext;
import com.pulsewatch.backend.alert.provider.AlertProvider;
import com.pulsewatch.backend.alert.repository.AlertHistoryRepository;
import com.pulsewatch.backend.alert.repository.AlertRuleRepository;
import com.pulsewatch.backend.exception.ResourceNotFoundException;
import com.pulsewatch.backend.incident.entity.Incident;
import com.pulsewatch.backend.incident.entity.IncidentEvent;
import com.pulsewatch.backend.incident.repository.IncidentEventRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.context.event.EventListener;
import com.pulsewatch.backend.incident.event.IncidentOpenedEvent;
import com.pulsewatch.backend.incident.event.IncidentResolvedEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    @Autowired private AlertRuleRepository alertRuleRepository;
    @Autowired private AlertHistoryRepository alertHistoryRepository;
    @Autowired private List<AlertProvider> alertProviders;
    @Autowired private MonitorRepository monitorRepository;
    @Autowired private IncidentEventRepository incidentEventRepository;

    /**
     * Called by IncidentService when an incident is opened.
     * Finds matching enabled rules, applies cooldown, sends alert, records history.
     */
    @Transactional
    @EventListener
    public void processIncidentOpened(IncidentOpenedEvent event) {
        Incident incident = event.getIncident();
        Monitor monitor = monitorRepository.findById(incident.getMonitorId()).orElse(null);
        String monitorName = monitor != null ? monitor.getName() : incident.getMonitorId().toString();

        List<AlertRule> rules = alertRuleRepository.findByMonitorIdAndRuleTypeAndEnabledTrue(
                incident.getMonitorId(), "INCIDENT_OPENED");

        for (AlertRule rule : rules) {
            if (!isCooldownExpired(rule)) {
                logger.info("Skipping alert (cooldown active) for monitor {} rule {}", incident.getMonitorId(), rule.getId());
                continue;
            }

            AlertContext ctx = new AlertContext();
            ctx.setMonitorId(incident.getMonitorId());
            ctx.setMonitorName(monitorName);
            ctx.setAlertType("INCIDENT_OPENED");
            ctx.setSeverity(incident.getSeverity());
            ctx.setRecipient(rule.getNotificationEmail());
            ctx.setIncidentId(incident.getId());
            ctx.setLayoutType(rule.getLayoutType());
            ctx.setCustomSubject(rule.getCustomSubject());
            ctx.setCustomBody(rule.getCustomBody());

            sendAndRecord(rule, ctx, incident.getId());
        }
    }

    /**
     * Called by IncidentService when an incident is resolved.
     */
    @Transactional
    @EventListener
    public void processIncidentResolved(IncidentResolvedEvent event) {
        Incident incident = event.getIncident();
        Monitor monitor = monitorRepository.findById(incident.getMonitorId()).orElse(null);
        String monitorName = monitor != null ? monitor.getName() : incident.getMonitorId().toString();

        List<AlertRule> rules = alertRuleRepository.findByMonitorIdAndRuleTypeAndEnabledTrue(
                incident.getMonitorId(), "INCIDENT_RESOLVED");

        for (AlertRule rule : rules) {
            AlertContext ctx = new AlertContext();
            ctx.setMonitorId(incident.getMonitorId());
            ctx.setMonitorName(monitorName);
            ctx.setAlertType("INCIDENT_RESOLVED");
            ctx.setSeverity(incident.getSeverity());
            ctx.setRecipient(rule.getNotificationEmail());
            ctx.setIncidentId(incident.getId());
            ctx.setDurationSeconds(incident.getDurationSeconds());
            ctx.setLayoutType(rule.getLayoutType());
            ctx.setCustomSubject(rule.getCustomSubject());
            ctx.setCustomBody(rule.getCustomBody());

            // No cooldown on resolved alerts — you always want to know when it recovered
            sendAndRecord(rule, ctx, incident.getId());
        }
    }

    /**
     * Sends a test email to verify email configuration.
     */
    public void sendTestAlert(String recipientEmail, UUID monitorId, UUID workspaceId) {
        Monitor monitor = monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found in active workspace"));

        AlertContext ctx = new AlertContext();
        ctx.setMonitorId(monitorId);
        ctx.setMonitorName(monitor.getName());
        ctx.setAlertType("TEST");
        ctx.setSeverity("INFO");
        ctx.setRecipient(recipientEmail);

        String status = "SENT";
        try {
            dispatchAlert(ctx);
        } catch (Exception e) {
            status = "FAILED";
            logger.error("Test alert failed for {}: {}", recipientEmail, e.getMessage());
        }

        AlertHistory history = new AlertHistory();
        history.setMonitorId(monitorId);
        history.setAlertType("TEST");
        history.setRecipient(recipientEmail);
        history.setSentAt(LocalDateTime.now());
        history.setStatus(status);
        alertHistoryRepository.save(history);
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    public AlertRule createRule(AlertRuleRequest request, UUID workspaceId) {
        // Verify monitor belongs to active workspace
        Monitor monitor = monitorRepository.findByIdAndWorkspaceId(request.getMonitorId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found in active workspace"));

        AlertRule rule = new AlertRule();
        rule.setWorkspaceId(monitor.getWorkspaceId()); // ← FIX: derive from monitor, not user JWT
        rule.setMonitorId(request.getMonitorId());
        rule.setRuleType(request.getRuleType());
        rule.setNotificationEmail(request.getNotificationEmail());
        rule.setCooldownMinutes(request.getCooldownMinutes() != null ? request.getCooldownMinutes() : 30);
        rule.setLayoutType(request.getLayoutType() != null ? request.getLayoutType() : "DEFAULT");
        rule.setCustomSubject(request.getCustomSubject());
        rule.setCustomBody(request.getCustomBody());
        rule.setEnabled(true);
        return alertRuleRepository.save(rule);

    }

    @Transactional
    public AlertRule updateRule(UUID ruleId, AlertRuleRequest request, UUID workspaceId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + ruleId));
        monitorRepository.findByIdAndWorkspaceId(rule.getMonitorId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + ruleId));

        rule.setRuleType(request.getRuleType());
        rule.setNotificationEmail(request.getNotificationEmail());
        rule.setCooldownMinutes(request.getCooldownMinutes() != null ? request.getCooldownMinutes() : 30);
        rule.setLayoutType(request.getLayoutType() != null ? request.getLayoutType() : "DEFAULT");
        rule.setCustomSubject(request.getCustomSubject());
        rule.setCustomBody(request.getCustomBody());

        return alertRuleRepository.save(rule);
    }

    public List<AlertRule> getRulesByMonitor(UUID monitorId, UUID workspaceId) {
        monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found in active workspace"));
        return alertRuleRepository.findByMonitorId(monitorId);
    }

    public Page<AlertRule> getAllRules(UUID workspaceId, Pageable pageable) {
        return alertRuleRepository.findByWorkspaceId(workspaceId, pageable);
    }

    public List<AlertRule> getAllRules() {
        return alertRuleRepository.findAll();
    }

    @Transactional
    public AlertRule toggleRule(UUID ruleId, UUID workspaceId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + ruleId));
        monitorRepository.findByIdAndWorkspaceId(rule.getMonitorId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + ruleId));
        rule.setEnabled(!rule.getEnabled());
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId, UUID workspaceId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + ruleId));
        monitorRepository.findByIdAndWorkspaceId(rule.getMonitorId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + ruleId));
        alertRuleRepository.delete(rule);
    }

    public Page<AlertHistory> getHistory(UUID workspaceId, String status, Pageable pageable) {
        return alertHistoryRepository.findByWorkspaceIdAndFilters(workspaceId, status, pageable);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private boolean isCooldownExpired(AlertRule rule) {
        if (rule.getLastAlertSentAt() == null) return true;
        LocalDateTime cooldownExpiry = rule.getLastAlertSentAt().plusMinutes(rule.getCooldownMinutes());
        return LocalDateTime.now().isAfter(cooldownExpiry);
    }

    private void dispatchAlert(AlertContext ctx) {
        AlertProvider provider = alertProviders.stream()
                .filter(p -> p.supports(ctx))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No matching alert provider found for recipient: " + ctx.getRecipient()));
        provider.sendAlert(ctx);
    }

    private void sendAndRecord(AlertRule rule, AlertContext ctx, UUID incidentId) {
        String status = "SENT";
        try {
            dispatchAlert(ctx);
            rule.setLastAlertSentAt(LocalDateTime.now());
            alertRuleRepository.save(rule);
        } catch (Exception e) {
            status = "FAILED";
            logger.error("Alert send failed for rule {} → {}: {}", rule.getId(), ctx.getRecipient(), e.getMessage());
        }

        // Record in alert_history
        AlertHistory history = new AlertHistory();
        history.setIncidentId(incidentId);
        history.setMonitorId(ctx.getMonitorId());
        history.setAlertType(ctx.getAlertType());
        history.setRecipient(ctx.getRecipient());
        history.setSentAt(LocalDateTime.now());
        history.setStatus(status);
        alertHistoryRepository.save(history);

        // Record ALERT_SENT event in incident_events for audit trail
        if (incidentId != null) {
            try {
                IncidentEvent event = new IncidentEvent(
                    incidentId,
                    "ALERT_SENT",
                    "Alert [" + ctx.getAlertType() + "] sent to " + ctx.getRecipient() + " — status: " + status
                );
                incidentEventRepository.save(event);
            } catch (Exception e) {
                logger.error("Failed to record ALERT_SENT event for incident {}: {}", incidentId, e.getMessage());
            }
        }
    }
}
