package com.pulsewatch.backend.export.controller;

import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.incident.entity.Incident;
import com.pulsewatch.backend.incident.service.IncidentService;
import com.pulsewatch.backend.monitor.entity.MonitorResult;
import com.pulsewatch.backend.monitor.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/export")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Export", description = "Export monitor history and incidents as CSV or JSON")
public class ExportController {

    @Autowired private IncidentService incidentService;
    @Autowired private MonitorService monitorService;

    // ── Incidents ─────────────────────────────────────────────────────────────

    @GetMapping("/incidents")
    @Operation(summary = "Export all incidents for the active workspace. format=csv|json")
    public ResponseEntity<byte[]> exportIncidents(
            @RequestParam(defaultValue = "csv") String format,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // Use workspaceId (not userId) — each user belongs to exactly one workspace.
        List<Incident> incidents = incidentService.getAllIncidents(userDetails.getWorkspaceId());

        if ("json".equalsIgnoreCase(format)) {
            String json = incidentsToJson(incidents);
            return download(json.getBytes(StandardCharsets.UTF_8), "incidents.json", "application/json");
        }

        String csv = incidentsToCsv(incidents);
        return download(csv.getBytes(StandardCharsets.UTF_8), "incidents.csv", "text/csv");
    }

    // ── Monitor History ───────────────────────────────────────────────────────

    @GetMapping("/monitor-history/{monitorId}")
    @Operation(summary = "Export check history for a monitor. format=csv|json, window=1h|24h|7d")
    public ResponseEntity<byte[]> exportMonitorHistory(
            @PathVariable UUID monitorId,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "24h") String window,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // getMonitorHistory validates that monitorId belongs to this workspace
        List<MonitorResult> results = monitorService.getMonitorHistory(monitorId, window, userDetails.getWorkspaceId());

        if ("json".equalsIgnoreCase(format)) {
            String json = historyToJson(results);
            return download(json.getBytes(StandardCharsets.UTF_8),
                    "monitor-history-" + monitorId + ".json", "application/json");
        }

        String csv = historyToCsv(results);
        return download(csv.getBytes(StandardCharsets.UTF_8),
                "monitor-history-" + monitorId + ".csv", "text/csv");
    }

    // ── Private: CSV builders ─────────────────────────────────────────────────

    private String incidentsToCsv(List<Incident> incidents) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,monitorId,status,severity,startedAt,resolvedAt,durationSeconds\n");
        for (Incident i : incidents) {
            sb.append(csv(i.getId()))
              .append(',').append(csv(i.getMonitorId()))
              .append(',').append(csv(i.getStatus()))
              .append(',').append(csv(i.getSeverity()))
              .append(',').append(csv(i.getStartedAt()))
              .append(',').append(csv(i.getResolvedAt()))
              .append(',').append(i.getDurationSeconds() != null ? i.getDurationSeconds() : "")
              .append('\n');
        }
        return sb.toString();
    }

    private String historyToCsv(List<MonitorResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,monitorId,checkedAt,statusCode,responseTimeMs,success\n");
        for (MonitorResult r : results) {
            sb.append(csv(r.getId()))
              .append(',').append(csv(r.getMonitorId()))
              .append(',').append(csv(r.getCheckedAt()))
              .append(',').append(r.getStatusCode() != null ? r.getStatusCode() : "")
              .append(',').append(r.getResponseTimeMs() != null ? r.getResponseTimeMs() : "")
              .append(',').append(r.isSuccess())
              .append('\n');
        }
        return sb.toString();
    }

    // ── Private: JSON builders (no external library) ──────────────────────────

    private String incidentsToJson(List<Incident> incidents) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < incidents.size(); i++) {
            Incident inc = incidents.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"id\":").append(json(inc.getId())).append(",")
              .append("\"monitorId\":").append(json(inc.getMonitorId())).append(",")
              .append("\"status\":").append(json(inc.getStatus())).append(",")
              .append("\"severity\":").append(json(inc.getSeverity())).append(",")
              .append("\"startedAt\":").append(json(inc.getStartedAt())).append(",")
              .append("\"resolvedAt\":").append(json(inc.getResolvedAt())).append(",")
              .append("\"durationSeconds\":").append(inc.getDurationSeconds())
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String historyToJson(List<MonitorResult> results) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            MonitorResult r = results.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"id\":").append(json(r.getId())).append(",")
              .append("\"monitorId\":").append(json(r.getMonitorId())).append(",")
              .append("\"checkedAt\":").append(json(r.getCheckedAt())).append(",")
              .append("\"statusCode\":").append(r.getStatusCode()).append(",")
              .append("\"responseTimeMs\":").append(r.getResponseTimeMs()).append(",")
              .append("\"success\":").append(r.isSuccess())
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── Private: helpers ──────────────────────────────────────────────────────

    private ResponseEntity<byte[]> download(byte[] data, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(data.length)
                .body(data);
    }

    /** CSV-safe string: null → empty, wrap in quotes, escape internal quotes. */
    private String csv(Object value) {
        if (value == null) return "";
        String s = value.toString().replace("\"", "\"\"");
        return s.contains(",") || s.contains("\n") || s.contains("\"") ? "\"" + s + "\"" : s;
    }

    /** JSON string: null → null literal, otherwise wrap in double quotes. */
    private String json(Object value) {
        if (value == null) return "null";
        return "\"" + value.toString().replace("\"", "\\\"") + "\"";
    }
}
