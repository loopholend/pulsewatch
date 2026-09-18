package com.pulsewatch.backend.scheduler;

import com.pulsewatch.backend.analytics.service.AnalyticsService;
import com.pulsewatch.backend.assertion.entity.MonitorAssertion;
import com.pulsewatch.backend.assertion.repository.MonitorAssertionRepository;
import com.pulsewatch.backend.assertion.service.AssertionEvaluator;
import com.pulsewatch.backend.incident.service.IncidentService;
import com.pulsewatch.backend.maintenance.service.MaintenanceWindowService;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.entity.MonitorResult;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import com.pulsewatch.backend.monitor.repository.MonitorResultRepository;
import com.pulsewatch.backend.statuspage.service.StatusPageService;
import com.pulsewatch.backend.realtime.service.RealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full execution pipeline for a single monitor check.
 *
 * Pipeline:
 *   1. Maintenance window check → skip if active
 *   2. HTTP check (captures status code + body)
 *   3. Assertion evaluation (STATUS_CODE, BODY_CONTAINS, BODY_NOT_CONTAINS)
 *   4. Save MonitorResult
 *   5. Update monitor status + consecutive failures
 *   6. Refresh analytics stats
 *   7. Sync service_status for status pages
 *   8. Incident handling (open / escalate / resolve)
 */
@Service
public class MonitorExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorExecutionService.class);
    private static final int MAX_BODY_LENGTH = 10_000; // truncate bodies > 10KB to avoid OOM

    /** Must match the same property used by IncidentService — single source of truth. */
    @Value("${pulsewatch.incident.failure-threshold:3}")
    int failureThreshold;

    @Autowired private MonitorRepository monitorRepository;
    @Autowired private MonitorResultRepository monitorResultRepository;
    @Autowired private MonitorAssertionRepository assertionRepository;
    @Autowired private MaintenanceWindowService maintenanceWindowService;
    @Autowired private AssertionEvaluator assertionEvaluator;
    @Autowired private AnalyticsService analyticsService;
    @Autowired @Lazy private IncidentService incidentService;
    @Autowired @Lazy private StatusPageService statusPageService;
    @Autowired private RealtimeService realtimeService;

    public void execute(Monitor monitor) {
        // ── 1. Maintenance window check ──────────────────────────────────────
        if (maintenanceWindowService.isInMaintenance(monitor.getId())) {
            handleMaintenance(monitor);
            return;
        }

        // ── 2. Perform HTTP check ────────────────────────────────────────────
        CheckResult check = performCheck(monitor);

        // ── 3. Evaluate assertions ───────────────────────────────────────────
        List<MonitorAssertion> assertions = assertionRepository.findByMonitorIdOrderByCreatedAtAsc(monitor.getId());
        boolean assertionsPassed = assertionEvaluator.evaluate(assertions, check.statusCode, check.body);
        boolean success = check.httpSuccess && assertionsPassed;

        String failureReason = null;
        if (!success) {
            if (check.errorMessage != null) {
                failureReason = check.errorMessage;
            } else if (check.statusCode > 0 && check.statusCode != monitor.getExpectedStatus()) {
                failureReason = "Expected " + monitor.getExpectedStatus() + ", got " + check.statusCode;
            } else if (!assertionsPassed) {
                failureReason = "Assertion validation failed";
            } else {
                failureReason = "Unknown failure";
            }
        }

        if (check.httpSuccess && !assertionsPassed) {
            logger.info("Monitor {} HTTP OK but assertions FAILED (status={})", monitor.getId(), check.statusCode);
        }

        // ── 4. Save result ───────────────────────────────────────────────────
        MonitorResult result = new MonitorResult();
        result.setMonitorId(monitor.getId());
        result.setStatusCode(check.statusCode > 0 ? check.statusCode : null);
        result.setResponseTimeMs(check.responseTimeMs);
        result.setFailureReason(failureReason);
        result.setSuccess(success);
        monitorResultRepository.save(result);

        // ── 5. Update monitor status ─────────────────────────────────────────
        updateMonitorStatus(monitor, success, failureReason);

        // ── 6. Refresh analytics ─────────────────────────────────────────────
        try {
            analyticsService.refreshMonitorStats(monitor.getId());
        } catch (Exception e) {
            logger.error("Failed to refresh stats for monitor {}: {}", monitor.getId(), e.getMessage());
        }

        // ── 7. Sync service_status ───────────────────────────────────────────
        try {
            statusPageService.syncMonitorStatus(monitor.getId(), monitor.getCurrentStatus());
        } catch (Exception e) {
            logger.error("Failed to sync service_status for monitor {}: {}", monitor.getId(), e.getMessage());
        }

        // ── 8. Broadcast Realtime Update ─────────────────────────────────────
        try {
            realtimeService.sendEvent(monitor.getWorkspaceId(), "MONITOR_UPDATED", monitor);
        } catch (Exception e) {
            logger.error("Failed to broadcast real-time update for monitor {}: {}", monitor.getId(), e.getMessage());
        }

        // ── 9. Structured Logging ────────────────────────────────────────────
        logger.info("Monitor executed: monitorId={}, status={}, latencyMs={}, success={}",
                monitor.getId(), check.statusCode, check.responseTimeMs, success);
    }

    // ── Private: HTTP check ──────────────────────────────────────────────────

    /**
     * Java 11 HttpClient used for all monitor checks.
     * <p>
     * WHY NOT WebClient + .block():
     * WebClient is built on Reactor Netty and is inherently non-blocking.
     * When called via .block() from a ScheduledExecutorService thread,
     * Reactor's .timeout() operator fires on a separate parallel scheduler,
     * but for certain sites (e.g. google.com, linkedin.com) that use
     * multi-hop TLS redirect chains, the cancellation signal never propagates
     * through Netty's internal redirect handler — causing the scheduler thread
     * to block indefinitely.
     * <p>
     * Java 11's HttpClient is synchronous and uses JVM-level interrupt-based
     * timeouts (.timeout() on the request). Thread.interrupt() always wakes
     * a parked thread regardless of what Netty's event loop is doing.
     * This is the correct tool for blocking monitor checks.
     */
    private static final java.net.http.HttpClient JAVA_HTTP_CLIENT =
            java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();

    private CheckResult performCheck(Monitor monitor) {
        long start = System.currentTimeMillis();
        CheckResult result = new CheckResult();

        try {
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(monitor.getUrl()))
                    .timeout(Duration.ofMillis(monitor.getTimeoutMs()))
                    .method(monitor.getMethod(),
                            java.net.http.HttpRequest.BodyPublishers.noBody());

            // Apply custom headers configured on the monitor
            Map<String, String> headers = monitor.getHeadersJson();
            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            java.net.http.HttpResponse<String> response =
                    JAVA_HTTP_CLIENT.send(requestBuilder.build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());

            result.responseTimeMs = (int) (System.currentTimeMillis() - start);
            result.statusCode     = response.statusCode();
            result.body           = truncateBody(response.body());
            result.httpSuccess    = (result.statusCode == monitor.getExpectedStatus());

        } catch (java.net.http.HttpTimeoutException e) {
            result.responseTimeMs = (int) (System.currentTimeMillis() - start);
            result.httpSuccess    = false;
            result.errorMessage   = "Timeout";
            logger.warn("Timeout checking monitor {}: {}ms elapsed", monitor.getId(), result.responseTimeMs);

        } catch (java.net.ConnectException e) {
            result.responseTimeMs = (int) (System.currentTimeMillis() - start);
            result.httpSuccess    = false;
            result.errorMessage   = "Connection Refused";
            logger.error("Error checking monitor {}: {}", monitor.getId(), e.getMessage());

        } catch (java.net.UnknownHostException e) {
            result.responseTimeMs = (int) (System.currentTimeMillis() - start);
            result.httpSuccess    = false;
            result.errorMessage   = "DNS Failure";
            logger.error("Error checking monitor {}: {}", monitor.getId(), e.getMessage());

        } catch (javax.net.ssl.SSLException e) {
            result.responseTimeMs = (int) (System.currentTimeMillis() - start);
            result.httpSuccess    = false;
            result.errorMessage   = "SSL Error";
            logger.error("Error checking monitor {}: {}", monitor.getId(), e.getMessage());

        } catch (Exception e) {
            result.responseTimeMs = (int) (System.currentTimeMillis() - start);
            result.httpSuccess    = false;
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.toLowerCase().contains("connection refused")) {
                result.errorMessage = "Connection Refused";
            } else if (msg.toLowerCase().contains("timeout") || msg.toLowerCase().contains("timed out")) {
                result.errorMessage = "Timeout";
            } else if (msg.toLowerCase().contains("unknownhost") || msg.toLowerCase().contains("unknown host")) {
                result.errorMessage = "DNS Failure";
            } else if (msg.toLowerCase().contains("ssl") || msg.toLowerCase().contains("handshake")
                    || msg.toLowerCase().contains("cert")) {
                result.errorMessage = "SSL Error";
            } else {
                result.errorMessage = msg.length() > 200 ? msg.substring(0, 200) : msg;
            }
            logger.error("Error checking monitor {}: {}", monitor.getId(), e.getMessage());
        }

        return result;
    }

    // ── Private: Status update + incident dispatch ───────────────────────────

    private void updateMonitorStatus(Monitor monitor, boolean success, String failureReason) {
        if (success) {
            monitor.setCurrentStatus("UP");
            monitor.setConsecutiveFailures(0);
            monitorRepository.save(monitor);
            try {
                incidentService.handleSuccess(monitor.getId());
            } catch (Exception e) {
                logger.error("Error on incident success for monitor {}: {}", monitor.getId(), e.getMessage());
            }
        } else {
            int failures = monitor.getConsecutiveFailures() + 1;
            monitor.setConsecutiveFailures(failures);
            if (failures >= failureThreshold) {
                monitor.setCurrentStatus("DOWN");
            }
            monitorRepository.save(monitor);
            try {
                incidentService.handleFailure(monitor.getId(), failures, failureReason);
            } catch (Exception e) {
                logger.error("Error on incident failure for monitor {}: {}", monitor.getId(), e.getMessage());
            }
        }
    }

    // ── Private: Maintenance handling ────────────────────────────────────────

    /**
     * During maintenance, record a synthetic "success" result to avoid triggering
     * incident creation. Set monitor status to MAINTENANCE and notify status pages.
     */
    private void handleMaintenance(Monitor monitor) {
        logger.debug("Monitor {} is in maintenance window — skipping incident logic", monitor.getId());

        monitor.setCurrentStatus("MAINTENANCE");
        monitorRepository.save(monitor);

        // Record a synthetic result so charts don't have gaps
        MonitorResult result = new MonitorResult();
        result.setMonitorId(monitor.getId());
        result.setSuccess(true);
        result.setResponseTimeMs(0);
        monitorResultRepository.save(result);

        try {
            statusPageService.syncMonitorStatus(monitor.getId(), "MAINTENANCE");
        } catch (Exception e) {
            logger.error("Failed to sync MAINTENANCE status for monitor {}: {}", monitor.getId(), e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String truncateBody(String body) {
        if (body == null) return null;
        return body.length() > MAX_BODY_LENGTH ? body.substring(0, MAX_BODY_LENGTH) : body;
    }

    /** Internal transfer object — not persisted. */
    private static class CheckResult {
        int statusCode;
        String body;
        int responseTimeMs;
        boolean httpSuccess;
        String errorMessage;
    }
}
