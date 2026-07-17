package com.pulsewatch.backend.monitor.controller;

import com.pulsewatch.backend.assertion.dto.MonitorAssertionRequest;
import com.pulsewatch.backend.assertion.entity.MonitorAssertion;

import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.monitor.dto.MonitorRequest;
import com.pulsewatch.backend.monitor.dto.MonitorOverviewResponse;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.entity.MonitorResult;
import com.pulsewatch.backend.monitor.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitors")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Monitors", description = "Monitor management APIs")
public class MonitorController {

    @Autowired private MonitorService monitorService;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new monitor")
    public ResponseEntity<Monitor> createMonitor(
            @Valid @RequestBody MonitorRequest request,
            @RequestParam(defaultValue = "false") boolean force,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.createMonitor(request, userDetails.getWorkspaceId(), userDetails.getId(), force));
    }

    @GetMapping
    @Operation(summary = "List monitors with optional search, status filter, and pagination")
    public ResponseEntity<Page<Monitor>> getMyMonitors(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(required = false)     String search,
            @RequestParam(required = false)     String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(monitorService.getUserMonitors(userDetails.getWorkspaceId(), search, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific monitor")
    public ResponseEntity<Monitor> getMonitor(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.getMonitorById(id, userDetails.getWorkspaceId()));
    }

    @GetMapping("/{id}/overview")
    @Operation(summary = "Get a consolidated health and statistics overview of a specific monitor")
    public ResponseEntity<MonitorOverviewResponse> getMonitorOverview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.getMonitorOverview(id, userDetails.getWorkspaceId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a monitor")
    public ResponseEntity<Monitor> updateMonitor(
            @PathVariable UUID id,
            @Valid @RequestBody MonitorRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.updateMonitor(id, request, userDetails.getWorkspaceId(), userDetails.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a monitor")
    public ResponseEntity<Map<String, String>> deleteMonitor(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        monitorService.deleteMonitor(id, userDetails.getWorkspaceId(), userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "Monitor deleted successfully"));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle monitor active state")
    public ResponseEntity<Monitor> toggleMonitor(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.toggleMonitor(id, userDetails.getWorkspaceId(), userDetails.getId()));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get uptime and latency stats for a monitor")
    public ResponseEntity<Map<String, Object>> getStats(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.getMonitorStats(id, userDetails.getWorkspaceId()));
    }

    // ── Draft Branching & Merging Mappings ──

    @PostMapping("/{id}/draft")
    @Operation(summary = "Create an isolated draft configuration branch for a monitor")
    public ResponseEntity<?> createDraft(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.createDraft(id, userDetails.getWorkspaceId(), userDetails.getId()));
    }

    @GetMapping("/{id}/draft")
    @Operation(summary = "Get the active draft configuration for a monitor")
    public ResponseEntity<?> getDraft(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.getDraft(id, userDetails.getWorkspaceId()));
    }

    @PostMapping("/{id}/draft/merge")
    @Operation(summary = "Merge the isolated draft configuration into the production monitor")
    public ResponseEntity<?> mergeDraft(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.mergeDraft(id, userDetails.getWorkspaceId(), userDetails.getId()));
    }

    @DeleteMapping("/{id}/draft")
    @Operation(summary = "Discard the active draft configuration branch")
    public ResponseEntity<?> discardDraft(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        monitorService.discardDraft(id, userDetails.getWorkspaceId());
        return ResponseEntity.ok(Map.of("message", "Draft branch discarded successfully"));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get the change timeline and incident logs of a specific monitor")
    public ResponseEntity<?> getTimeline(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.getMonitorTimeline(id, userDetails.getWorkspaceId()));
    }

    // ── History with time window ──────────────────────────────────────────────

    @GetMapping("/{id}/history")
    @Operation(summary = "Get check results. window=1h|24h|7d (default 24h)")
    public ResponseEntity<List<MonitorResult>> getHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "24h") String window,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.getMonitorHistory(id, window, userDetails.getWorkspaceId()));
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/assertions")
    @Operation(summary = "Add an assertion to a monitor")
    public ResponseEntity<MonitorAssertion> addAssertion(
            @PathVariable UUID id,
            @RequestBody MonitorAssertionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.addAssertion(id, request, userDetails.getWorkspaceId()));
    }

    @GetMapping("/{id}/assertions")
    @Operation(summary = "List assertions for a monitor")
    public ResponseEntity<List<MonitorAssertion>> listAssertions(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(monitorService.getAssertions(id, userDetails.getWorkspaceId()));
    }

    @DeleteMapping("/{monitorId}/assertions/{assertionId}")
    @Operation(summary = "Delete an assertion")
    public ResponseEntity<Void> deleteAssertion(
            @PathVariable UUID monitorId,
            @PathVariable UUID assertionId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        monitorService.deleteAssertion(monitorId, assertionId, userDetails.getWorkspaceId());
        return ResponseEntity.noContent().build();
    }
}
