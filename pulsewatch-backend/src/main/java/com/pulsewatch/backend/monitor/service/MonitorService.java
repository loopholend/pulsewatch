package com.pulsewatch.backend.monitor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsewatch.backend.assertion.dto.MonitorAssertionRequest;
import com.pulsewatch.backend.assertion.entity.MonitorAssertion;
import com.pulsewatch.backend.assertion.repository.MonitorAssertionRepository;
import com.pulsewatch.backend.audit.service.AuditLogService;
import com.pulsewatch.backend.exception.ResourceNotFoundException;
import com.pulsewatch.backend.exception.DuplicateMonitorException;
import com.pulsewatch.backend.monitor.dto.MonitorRequest;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.entity.MonitorResult;
import com.pulsewatch.backend.monitor.entity.MonitorDraft;
import com.pulsewatch.backend.monitor.entity.MonitorTimelineEvent;
import com.pulsewatch.backend.monitor.entity.WorkspaceActivity;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import com.pulsewatch.backend.monitor.repository.MonitorResultRepository;
import com.pulsewatch.backend.monitor.repository.MonitorDraftRepository;
import com.pulsewatch.backend.monitor.repository.MonitorTimelineEventRepository;
import com.pulsewatch.backend.monitor.repository.WorkspaceActivityRepository;
import com.pulsewatch.backend.auth.entity.User;
import com.pulsewatch.backend.auth.repository.UserRepository;
import com.pulsewatch.backend.scheduler.MonitorScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewatch.backend.analytics.entity.MonitorStats;
import com.pulsewatch.backend.analytics.repository.MonitorStatsRepository;
import com.pulsewatch.backend.incident.entity.Incident;
import com.pulsewatch.backend.incident.repository.IncidentRepository;
import com.pulsewatch.backend.monitor.dto.MonitorOverviewResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MonitorService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> VALID_ASSERT_TYPES =
            Set.of("STATUS_CODE", "BODY_CONTAINS", "BODY_NOT_CONTAINS");
    private static final Set<String> VALID_OPERATORS =
            Set.of("EQ", "CONTAINS", "NOT_CONTAINS");

    @Autowired private MonitorRepository monitorRepository;
    @Autowired private MonitorResultRepository monitorResultRepository;
    @Autowired private MonitorAssertionRepository assertionRepository;
    @Autowired private AuditLogService auditLogService;
    @Autowired private MonitorStatsRepository monitorStatsRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private MonitorScheduler monitorScheduler;
    @Autowired private MonitorDraftRepository monitorDraftRepository;
    @Autowired private MonitorTimelineEventRepository monitorTimelineEventRepository;
    @Autowired private WorkspaceActivityRepository workspaceActivityRepository;
    @Autowired private UserRepository userRepository;

    public MonitorOverviewResponse getMonitorOverview(UUID id, UUID workspaceId) {
        Monitor monitor = getMonitorById(id, workspaceId);

        MonitorOverviewResponse response = new MonitorOverviewResponse();
        response.setId(monitor.getId());
        response.setName(monitor.getName());
        response.setUrl(monitor.getUrl());
        response.setMonitorType(monitor.getMonitorType());
        response.setMethod(monitor.getMethod());
        response.setIntervalSeconds(monitor.getIntervalSeconds());
        response.setTimeoutMs(monitor.getTimeoutMs());
        response.setExpectedStatus(monitor.getExpectedStatus());
        response.setCurrentStatus(monitor.getCurrentStatus());
        response.setActive(monitor.isActive());

        // Fetch stats
        MonitorStats stats = monitorStatsRepository.findById(id).orElse(new MonitorStats(id));
        response.setUptime(stats.getUptime() != null ? stats.getUptime() : 0.0);
        response.setAvgLatency(stats.getAvgLatency() != null ? stats.getAvgLatency() : 0);
        response.setP95Latency(stats.getP95Latency() != null ? stats.getP95Latency() : 0);
        response.setTotalChecks(stats.getTotalChecks() != null ? stats.getTotalChecks() : 0L);
        response.setSuccessfulChecks(stats.getSuccessfulChecks() != null ? stats.getSuccessfulChecks() : 0L);
        response.setFailedChecks(stats.getFailedChecks() != null ? stats.getFailedChecks() : 0L);

        // Fetch latest check result — single-row fetch, no Top100 waste
        MonitorResult lastResult = monitorResultRepository.findFirstByMonitorIdOrderByCheckedAtDesc(id);
        if (lastResult != null) {
            response.setLastStatusCode(lastResult.getStatusCode());
            response.setLastLatencyMs(lastResult.getResponseTimeMs());
            response.setLastCheckedAt(lastResult.getCheckedAt());
            response.setNextCheckAt(lastResult.getCheckedAt().plusSeconds(monitor.getIntervalSeconds()));
            response.setLastFailureReason(lastResult.getFailureReason());
        }

        // Fetch last successful check details
        MonitorResult lastSuccess = monitorResultRepository.findFirstByMonitorIdAndSuccessTrueOrderByCheckedAtDesc(id);
        if (lastSuccess != null) {
            response.setLastSuccessfulCheckAt(lastSuccess.getCheckedAt());
            response.setLastSuccessfulLatencyMs(lastSuccess.getResponseTimeMs());
        }

        response.setConsecutiveFailures(monitor.getConsecutiveFailures());

        // Fetch current incident — single-row fetch, no full list load
        Optional<Incident> latestIncident = incidentRepository.findFirstByMonitorIdOrderByStartedAtDesc(id);
        if (latestIncident.isPresent() && "OPEN".equals(latestIncident.get().getStatus())) {
            Incident inc = latestIncident.get();
            response.setCurrentIncidentStatus(inc.getStatus());
            response.setCurrentIncidentSeverity(inc.getSeverity());
            response.setCurrentIncidentStartedAt(inc.getStartedAt());
        }

        return response;
    }

    private String getUserEmail(UUID userId) {
        if (userId == null) return "System";
        return userRepository.findById(userId).map(User::getEmail).orElse("Unknown User");
    }

    private void logActivity(UUID workspaceId, UUID userId, String actionType, String description) {
        WorkspaceActivity activity = new WorkspaceActivity(workspaceId, userId, actionType, description);
        workspaceActivityRepository.save(activity);
    }

    private void logTimeline(UUID monitorId, UUID workspaceId, String eventType, String message, UUID userId) {
        MonitorTimelineEvent event = new MonitorTimelineEvent(monitorId, workspaceId, eventType, message, userId);
        monitorTimelineEventRepository.save(event);
    }

    @Transactional
    public Monitor createMonitor(MonitorRequest request, UUID workspaceId, UUID userId, boolean force) {
        if (!force) {
            List<Monitor> existing = monitorRepository.findByWorkspaceIdAndUrlAndActiveTrue(workspaceId, request.getUrl());
            if (!existing.isEmpty()) {
                throw new DuplicateMonitorException(existing.get(0));
            }
        }

        Monitor monitor = new Monitor();
        monitor.setWorkspaceId(workspaceId);
        monitor.setCreatedBy(userId);
        monitor.setLastModifiedBy(userId);
        mapRequestToMonitor(request, monitor);
        Monitor saved = monitorRepository.save(monitor);
        
        auditLogService.log(userId, "CREATE_MONITOR", "MONITOR", saved.getId());
        
        String email = getUserEmail(userId);
        logActivity(workspaceId, userId, "CREATE_MONITOR", email + " created monitor " + saved.getName());
        logTimeline(saved.getId(), workspaceId, "CREATED", "Monitor created by " + email, userId);

        monitorScheduler.scheduleMonitor(saved);
        return saved;
    }

    /** Paginated listing with optional name search and status filter. */
    public Page<Monitor> getUserMonitors(UUID workspaceId, String search, String status, Pageable pageable) {
        String normalizedSearch = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;
        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        return monitorRepository.findByFilters(workspaceId, normalizedStatus, normalizedSearch, pageable);
    }

    /** Non-paginated listing kept for internal use (scheduler, etc.) */
    public List<Monitor> getUserMonitors(UUID workspaceId) {
        return monitorRepository.findByWorkspaceId(workspaceId);
    }

    public Monitor getMonitorById(UUID id, UUID workspaceId) {
        return monitorRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found in active workspace"));
    }

    @Transactional
    public Monitor updateMonitor(UUID id, MonitorRequest request, UUID workspaceId, UUID userId) {
        Monitor monitor = getMonitorById(id, workspaceId);
        monitor.setLastModifiedBy(userId);
        mapRequestToMonitor(request, monitor);
        Monitor updated = monitorRepository.save(monitor);
        
        auditLogService.log(userId, "UPDATE_MONITOR", "MONITOR", id);

        String email = getUserEmail(userId);
        logActivity(workspaceId, userId, "UPDATE_MONITOR", email + " updated monitor " + updated.getName());
        logTimeline(id, workspaceId, "CONFIG_CHANGED", "Configuration updated by " + email, userId);

        // Re-schedule to pick up any interval or configuration changes.
        if (updated.isActive()) {
            monitorScheduler.scheduleMonitor(updated);
        }
        return updated;
    }

    @Transactional
    public void deleteMonitor(UUID id, UUID workspaceId, UUID userId) {
        Monitor monitor = getMonitorById(id, workspaceId);
        monitorScheduler.unscheduleMonitor(id);
        
        monitorDraftRepository.findByMonitorId(id).ifPresent(monitorDraftRepository::delete);
        monitorRepository.delete(monitor);
        
        auditLogService.log(userId, "DELETE_MONITOR", "MONITOR", id);

        String email = getUserEmail(userId);
        logActivity(workspaceId, userId, "DELETE_MONITOR", email + " deleted monitor " + monitor.getName());
    }

    @Transactional
    public Monitor toggleMonitor(UUID id, UUID workspaceId, UUID userId) {
        Monitor monitor = getMonitorById(id, workspaceId);
        monitor.setActive(!monitor.isActive());
        monitor.setLastModifiedBy(userId);
        Monitor saved = monitorRepository.save(monitor);

        String email = getUserEmail(userId);
        String state = saved.isActive() ? "resumed" : "paused";
        String eventType = saved.isActive() ? "RESUMED" : "PAUSED";

        logActivity(workspaceId, userId, "PAUSE_MONITOR", email + " " + state + " monitor " + saved.getName());
        logTimeline(id, workspaceId, eventType, "Monitor " + state + " by " + email, userId);

        if (saved.isActive()) {
            monitorScheduler.scheduleMonitor(saved);
        } else {
            monitorScheduler.unscheduleMonitor(id);
        }
        return saved;
    }

    public Map<String, Object> getMonitorStats(UUID id, UUID workspaceId) {
        getMonitorById(id, workspaceId);
        long total = monitorResultRepository.countByMonitorId(id);
        long success = monitorResultRepository.countSuccessByMonitorId(id);
        Double avgLatency = monitorResultRepository.avgResponseTimeByMonitorId(id);
        double uptime = total == 0 ? 0 : (double) success / total * 100;
        double errorRate = total == 0 ? 0 : (double) (total - success) / total * 100;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChecks", total);
        stats.put("successChecks", success);
        stats.put("uptime", Math.round(uptime * 100.0) / 100.0);
        stats.put("errorRate", Math.round(errorRate * 100.0) / 100.0);
        stats.put("avgLatency", avgLatency != null ? Math.round(avgLatency) : 0);
        return stats;
    }

    /**
     * Time-windowed history. window values: 1h, 24h, 7d (default: 24h).
     */
    public List<MonitorResult> getMonitorHistory(UUID id, String window, UUID workspaceId) {
        getMonitorById(id, workspaceId);
        LocalDateTime since = switch (window.toLowerCase()) {
            case "1h"  -> LocalDateTime.now().minusHours(1);
            case "7d"  -> LocalDateTime.now().minusDays(7);
            default    -> LocalDateTime.now().minusHours(24);
        };
        return monitorResultRepository.findByMonitorIdAndCheckedAtAfterOrderByCheckedAtDesc(id, since);
    }

    // ── Draft Branching and Merging ───────────────────────────────────────────

    @Transactional
    public MonitorDraft createDraft(UUID monitorId, UUID workspaceId, UUID userId) {
        Monitor monitor = getMonitorById(monitorId, workspaceId);
        monitorDraftRepository.findByMonitorId(monitorId).ifPresent(monitorDraftRepository::delete);

        List<MonitorAssertion> assertions = getAssertions(monitorId, workspaceId);

        // Use ObjectMapper — prevents JSON injection if 'expected' contains quotes
        List<Map<String, String>> assertionList = assertions.stream()
                .map(ma -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("assertType", ma.getAssertType());
                    m.put("operator", ma.getOperator());
                    m.put("expected", ma.getExpected());
                    return m;
                })
                .collect(Collectors.toList());
        String assertionsJsonStr;
        try {
            assertionsJsonStr = objectMapper.writeValueAsString(assertionList);
        } catch (Exception e) {
            logger.warn("Failed to serialize assertions for draft of monitor {}: {}", monitorId, e.getMessage());
            assertionsJsonStr = "[]";
        }

        String headersJson = "";
        if (monitor.getHeadersJson() != null) {
            try {
                headersJson = objectMapper.writeValueAsString(monitor.getHeadersJson());
            } catch (Exception e) {
                headersJson = "{}";
            }
        }

        MonitorDraft draft = new MonitorDraft();
        draft.setMonitorId(monitorId);
        draft.setWorkspaceId(workspaceId);
        draft.setName(monitor.getName());
        draft.setUrl(monitor.getUrl());
        draft.setMethod(monitor.getMethod());
        draft.setHeadersJson(headersJson);
        draft.setAssertionsJson(assertionsJsonStr);
        draft.setIntervalSeconds(monitor.getIntervalSeconds());
        draft.setTimeoutMs(monitor.getTimeoutMs());
        draft.setExpectedStatus(monitor.getExpectedStatus());
        draft.setNotes("Experimental configuration branch");
        draft.setLastModifiedBy(userId);

        return monitorDraftRepository.save(draft);
    }

    public MonitorDraft getDraft(UUID monitorId, UUID workspaceId) {
        return monitorDraftRepository.findByMonitorIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("No active draft found for monitor: " + monitorId));
    }

    @Transactional
    public Monitor mergeDraft(UUID monitorId, UUID workspaceId, UUID userId) {
        MonitorDraft draft = getDraft(monitorId, workspaceId);
        Monitor monitor = getMonitorById(monitorId, workspaceId);

        monitor.setName(draft.getName());
        monitor.setUrl(draft.getUrl());
        monitor.setMethod(draft.getMethod());
        monitor.setIntervalSeconds(draft.getIntervalSeconds());
        monitor.setTimeoutMs(draft.getTimeoutMs());
        monitor.setExpectedStatus(draft.getExpectedStatus());
        monitor.setLastModifiedBy(userId);

        if (draft.getHeadersJson() != null && !draft.getHeadersJson().isBlank()) {
            try {
                Map<String, String> headers = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(draft.getHeadersJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                monitor.setHeadersJson(headers);
            } catch (Exception e) {
                logger.warn("Failed to parse draft headersJson for monitor {}: {}", monitorId, e.getMessage());
            }
        }

        if (draft.getAssertionsJson() != null && !draft.getAssertionsJson().isBlank()) {
            assertionRepository.deleteAllByMonitorId(monitorId);
            try {
                List<Map<String, String>> list = objectMapper
                        .readValue(draft.getAssertionsJson(), new TypeReference<List<Map<String, String>>>() {});
                for (Map<String, String> map : list) {
                    MonitorAssertion ma = new MonitorAssertion();
                    ma.setMonitorId(monitorId);
                    ma.setAssertType(map.get("assertType"));
                    ma.setOperator(map.get("operator"));
                    ma.setExpected(map.get("expected"));
                    assertionRepository.save(ma);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse draft assertionsJson for monitor {}: {}", monitorId, e.getMessage());
                throw new IllegalArgumentException("Draft assertions contain invalid JSON. Please discard and re-create the draft.");
            }
        }

        Monitor merged = monitorRepository.save(monitor);

        if (merged.isActive()) {
            monitorScheduler.scheduleMonitor(merged);
        }

        String email = getUserEmail(userId);
        logActivity(workspaceId, userId, "MERGE_DRAFT", email + " merged draft for monitor " + merged.getName());
        logTimeline(monitorId, workspaceId, "DRAFT_MERGED", "Draft configurations merged by " + email, userId);

        monitorDraftRepository.delete(draft);

        return merged;
    }

    @Transactional
    public void discardDraft(UUID monitorId, UUID workspaceId) {
        MonitorDraft draft = getDraft(monitorId, workspaceId);
        monitorDraftRepository.delete(draft);
    }

    public List<MonitorTimelineEvent> getMonitorTimeline(UUID monitorId, UUID workspaceId) {
        getMonitorById(monitorId, workspaceId);
        return monitorTimelineEventRepository.findByMonitorIdAndWorkspaceIdOrderByCreatedAtDesc(monitorId, workspaceId);
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    @Transactional
    public MonitorAssertion addAssertion(UUID monitorId, MonitorAssertionRequest request, UUID workspaceId) {
        getMonitorById(monitorId, workspaceId);

        if (!VALID_ASSERT_TYPES.contains(request.getAssertType())) {
            throw new IllegalArgumentException("Invalid assertType. Allowed: STATUS_CODE, BODY_CONTAINS, BODY_NOT_CONTAINS");
        }
        if (!VALID_OPERATORS.contains(request.getOperator())) {
            throw new IllegalArgumentException("Invalid operator. Allowed: EQ, CONTAINS, NOT_CONTAINS");
        }
        if (request.getExpected() == null || request.getExpected().isBlank()) {
            throw new IllegalArgumentException("expected value is required");
        }

        MonitorAssertion assertion = new MonitorAssertion();
        assertion.setMonitorId(monitorId);
        assertion.setAssertType(request.getAssertType());
        assertion.setOperator(request.getOperator());
        assertion.setExpected(request.getExpected().trim());
        return assertionRepository.save(assertion);
    }

    public List<MonitorAssertion> getAssertions(UUID monitorId, UUID workspaceId) {
        getMonitorById(monitorId, workspaceId);
        return assertionRepository.findByMonitorIdOrderByCreatedAtAsc(monitorId);
    }

    @Transactional
    public void deleteAssertion(UUID monitorId, UUID assertionId, UUID workspaceId) {
        getMonitorById(monitorId, workspaceId);
        MonitorAssertion assertion = assertionRepository.findById(assertionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assertion not found: " + assertionId));
        if (!assertion.getMonitorId().equals(monitorId)) {
            throw new ResourceNotFoundException("Assertion not found: " + assertionId);
        }
        assertionRepository.delete(assertion);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void mapRequestToMonitor(MonitorRequest request, Monitor monitor) {
        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        monitor.setMonitorType(request.getMonitorType());
        monitor.setMethod(request.getMethod());
        monitor.setHeadersJson(request.getHeadersJson());
        monitor.setIntervalSeconds(request.getIntervalSeconds());
        monitor.setTimeoutMs(request.getTimeoutMs());
        monitor.setExpectedStatus(request.getExpectedStatus());
    }
}
