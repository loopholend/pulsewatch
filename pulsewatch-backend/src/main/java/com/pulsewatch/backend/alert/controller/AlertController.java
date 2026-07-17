package com.pulsewatch.backend.alert.controller;

import com.pulsewatch.backend.alert.dto.AlertRuleRequest;
import com.pulsewatch.backend.alert.dto.TestAlertRequest;
import com.pulsewatch.backend.alert.entity.AlertHistory;
import com.pulsewatch.backend.alert.entity.AlertRule;

import com.pulsewatch.backend.alert.service.AlertService;
import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Alerts", description = "Alert Rule Management APIs")
public class AlertController {

    @Autowired private AlertService alertService;

    @PostMapping("/rules")
    @Operation(summary = "Create an alert rule for a monitor")
    public ResponseEntity<AlertRule> createRule(
            @RequestBody AlertRuleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(alertService.createRule(request, userDetails.getWorkspaceId()));
    }

    @PutMapping("/rules/{id}")
    @Operation(summary = "Update an alert rule (including custom templates)")
    public ResponseEntity<AlertRule> updateRule(
            @PathVariable UUID id,
            @RequestBody AlertRuleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(alertService.updateRule(id, request, userDetails.getWorkspaceId()));
    }

    @GetMapping("/rules")
    @Operation(summary = "List all alert rules (paginated)")
    public ResponseEntity<Page<AlertRule>> getAllRules(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(alertService.getAllRules(userDetails.getWorkspaceId(), pageable));
    }

    @GetMapping("/rules/monitor/{monitorId}")
    @Operation(summary = "List alert rules for a specific monitor")
    public ResponseEntity<List<AlertRule>> getRulesByMonitor(
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(alertService.getRulesByMonitor(monitorId, userDetails.getWorkspaceId()));
    }

    @PatchMapping("/rules/{id}")
    @Operation(summary = "Toggle enabled/disabled state of an alert rule")
    public ResponseEntity<AlertRule> toggleRule(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(alertService.toggleRule(id, userDetails.getWorkspaceId()));
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "Delete an alert rule permanently")
    public ResponseEntity<Void> deleteRule(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        alertService.deleteRule(id, userDetails.getWorkspaceId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    @Operation(summary = "Get alert history (paginated, optional status filter)")
    public ResponseEntity<Page<AlertHistory>> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        return ResponseEntity.ok(alertService.getHistory(userDetails.getWorkspaceId(), normalizedStatus, pageable));
    }

    @PostMapping("/test")
    @Operation(summary = "Send a test alert email to verify configuration")
    public ResponseEntity<Map<String, String>> testAlert(
            @RequestBody TestAlertRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        alertService.sendTestAlert(request.getEmail(), request.getMonitorId(), userDetails.getWorkspaceId());
        return ResponseEntity.ok(Map.of("message", "Test alert sent to " + request.getEmail()));
    }
}
