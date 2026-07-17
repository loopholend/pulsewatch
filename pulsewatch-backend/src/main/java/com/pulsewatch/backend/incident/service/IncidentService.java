package com.pulsewatch.backend.incident.service;


import com.pulsewatch.backend.incident.entity.Incident;
import com.pulsewatch.backend.incident.entity.IncidentEvent;
import com.pulsewatch.backend.incident.repository.IncidentEventRepository;
import com.pulsewatch.backend.incident.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import com.pulsewatch.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.pulsewatch.backend.incident.event.IncidentOpenedEvent;
import com.pulsewatch.backend.incident.event.IncidentResolvedEvent;
import org.springframework.transaction.annotation.Transactional;
import com.pulsewatch.backend.realtime.service.RealtimeService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentService.class);

    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentEventRepository incidentEventRepository;
    @Autowired private MonitorRepository monitorRepository;
    @Autowired private RealtimeService realtimeService;

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Value("${pulsewatch.incident.failure-threshold:3}")
    private int failureThreshold;

    @Transactional
    public void handleFailure(UUID monitorId, int consecutiveFailures, String failureReason) {
        if (consecutiveFailures < failureThreshold) {
            return; // Don't open incident until threshold is reached
        }

        Optional<Incident> openIncidentOpt = incidentRepository
                .findFirstByMonitorIdAndStatusInOrderByStartedAtDesc(monitorId, List.of("OPEN", "ACKNOWLEDGED"));

        if (openIncidentOpt.isEmpty()) {
            Monitor monitor = monitorRepository.findById(monitorId).orElse(null);
            UUID workspaceId = monitor != null ? monitor.getWorkspaceId() : null;
            if (workspaceId == null) {
                logger.error("Cannot open incident: monitor {} has no workspace", monitorId);
                return;
            }
            Incident incident = new Incident();
            incident.setMonitorId(monitorId);
            incident.setWorkspaceId(workspaceId);
            incident.setStatus("OPEN");
            incident.setStartedAt(LocalDateTime.now());
            incident.setSeverity(calculateSeverity(consecutiveFailures));
            incident.setFailureReason(failureReason);
            incidentRepository.save(incident);

            // Record event
            recordEvent(incident.getId(), "INCIDENT_OPENED",
                    "Incident opened after " + consecutiveFailures + " consecutive failures. Severity: " + incident.getSeverity());

            logger.info("Opened new incident for monitor {} with severity {}", monitorId, incident.getSeverity());

            // Trigger alert
            try {
                eventPublisher.publishEvent(new IncidentOpenedEvent(this, incident));
            } catch (Exception e) {
                logger.error("Event publish failed for INCIDENT_OPENED on monitor {}: {}", monitorId, e.getMessage());
            }

            broadcastIncidentUpdate(incident);

        } else {
            Incident incident = openIncidentOpt.get();
            String newSeverity = calculateSeverity(consecutiveFailures);
            if (!incident.getSeverity().equals(newSeverity)) {
                String oldSeverity = incident.getSeverity();
                incident.setSeverity(newSeverity);
                incidentRepository.save(incident);

                // Record escalation event
                recordEvent(incident.getId(), "SEVERITY_ESCALATED",
                        "Severity escalated from " + oldSeverity + " to " + newSeverity +
                        " after " + consecutiveFailures + " consecutive failures.");

                logger.info("Escalated incident for monitor {} to severity {}", monitorId, newSeverity);
                broadcastIncidentUpdate(incident);
            }
        }
    }

    @Transactional
    public void handleSuccess(UUID monitorId) {
        Optional<Incident> openIncidentOpt = incidentRepository
                .findFirstByMonitorIdAndStatusInOrderByStartedAtDesc(monitorId, List.of("OPEN", "ACKNOWLEDGED"));

        if (openIncidentOpt.isPresent()) {
            Incident incident = openIncidentOpt.get();
            incident.setStatus("RESOLVED");
            incident.setResolvedAt(LocalDateTime.now());

            long duration = Duration.between(incident.getStartedAt(), incident.getResolvedAt()).getSeconds();
            incident.setDurationSeconds(duration);
            incidentRepository.save(incident);

            // Record event
            recordEvent(incident.getId(), "INCIDENT_RESOLVED",
                    "Incident resolved. Total downtime: " + duration + " seconds.");

            logger.info("Resolved incident for monitor {}. Duration: {} seconds", monitorId, duration);

            // Trigger alert
            try {
                eventPublisher.publishEvent(new IncidentResolvedEvent(this, incident));
            } catch (Exception e) {
                logger.error("Event publish failed for INCIDENT_RESOLVED on monitor {}: {}", monitorId, e.getMessage());
            }

            broadcastIncidentUpdate(incident);
        }
    }

    @Transactional
    public Incident acknowledgeIncident(UUID incidentId, UUID workspaceId) {
        Incident incident = getIncidentById(incidentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
        if (!"OPEN".equals(incident.getStatus())) {
            throw new IllegalStateException("Only open incidents can be acknowledged.");
        }
        incident.setStatus("ACKNOWLEDGED");
        Incident saved = incidentRepository.save(incident);
        recordEvent(incidentId, "INCIDENT_ACKNOWLEDGED", "Incident acknowledged.");
        broadcastIncidentUpdate(saved);
        return saved;
    }

    @Transactional
    public Incident resolveIncident(UUID incidentId, UUID workspaceId) {
        Incident incident = getIncidentById(incidentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
        if ("RESOLVED".equals(incident.getStatus())) {
            throw new IllegalStateException("Incident is already resolved.");
        }
        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        long duration = Duration.between(incident.getStartedAt(), incident.getResolvedAt()).getSeconds();
        incident.setDurationSeconds(duration);
        Incident saved = incidentRepository.save(incident);
        recordEvent(incidentId, "INCIDENT_RESOLVED", "Incident manually resolved.");

        try {
            eventPublisher.publishEvent(new IncidentResolvedEvent(this, saved));
        } catch (Exception e) {
            logger.error("Event publish failed for INCIDENT_RESOLVED on manual resolve: {}", e.getMessage());
        }

        broadcastIncidentUpdate(saved);
        return saved;
    }

    private void broadcastIncidentUpdate(Incident incident) {
        if (incident == null) return;
        try {
            realtimeService.sendEvent(incident.getWorkspaceId(), "INCIDENT_UPDATED", incident);
        } catch (Exception e) {
            logger.error("Failed to broadcast real-time incident update: {}", e.getMessage());
        }
    }

    public Page<Incident> getAllIncidents(UUID workspaceId, String status, String severity, Pageable pageable) {
        String s = (status != null && !status.isBlank())   ? status.trim().toUpperCase()   : null;
        String v = (severity != null && !severity.isBlank()) ? severity.trim().toUpperCase() : null;
        return incidentRepository.findByWorkspaceIdAndFilters(workspaceId, s, v, pageable);
    }

    /** Non-paginated — kept for internal use. */
    public List<Incident> getAllIncidents(UUID workspaceId) {
        return incidentRepository.findByWorkspaceId(workspaceId);
    }

    public List<Incident> getIncidentsByMonitor(UUID monitorId, UUID workspaceId) {
        monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
        return incidentRepository.findByMonitorIdOrderByStartedAtDesc(monitorId);
    }

    public Optional<Incident> getIncidentById(UUID incidentId, UUID workspaceId) {
        return incidentRepository.findByIdAndWorkspaceId(incidentId, workspaceId);
    }

    public List<IncidentEvent> getEventsByIncident(UUID incidentId, UUID workspaceId) {
        getIncidentById(incidentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        return incidentEventRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void recordEvent(UUID incidentId, String eventType, String message) {
        try {
            IncidentEvent event = new IncidentEvent(incidentId, eventType, message);
            incidentEventRepository.save(event);
        } catch (Exception e) {
            logger.error("Failed to record incident event {}: {}", eventType, e.getMessage());
        }
    }

    private String calculateSeverity(int failures) {
        if (failures >= 10) return "CRITICAL";
        if (failures >= 5)  return "HIGH";
        if (failures >= 3)  return "MEDIUM";
        return "LOW";
    }
}
