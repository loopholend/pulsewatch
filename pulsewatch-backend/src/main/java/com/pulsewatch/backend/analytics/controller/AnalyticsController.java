package com.pulsewatch.backend.analytics.controller;

import com.pulsewatch.backend.analytics.dto.DashboardSummaryResponse;
import com.pulsewatch.backend.analytics.dto.LatencyDataPoint;
import com.pulsewatch.backend.analytics.dto.MonitorStatsResponse;
import com.pulsewatch.backend.analytics.service.AnalyticsService;
import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics", description = "Monitor analytics and statistics APIs")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary (totals, uptime, 24h latency)")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(analyticsService.getDashboardSummary(userDetails.getWorkspaceId()));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get stats for all monitors (dashboard overview)")
    public ResponseEntity<List<MonitorStatsResponse>> getDashboard(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(analyticsService.getAllStats(userDetails.getWorkspaceId()));
    }

    @GetMapping("/{monitorId}")
    @Operation(summary = "Get stats for a specific monitor")
    public ResponseEntity<MonitorStatsResponse> getMonitorStats(
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(analyticsService.getStats(monitorId, userDetails.getWorkspaceId()));
    }

    @GetMapping("/{monitorId}/latency")
    @Operation(summary = "Get latency history for charts")
    public ResponseEntity<List<LatencyDataPoint>> getLatencyTrend(
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(analyticsService.getLatencyTrend(monitorId, userDetails.getWorkspaceId(), 1));
    }
    
    @GetMapping("/{monitorId}/uptime")
    @Operation(summary = "Get uptime history for charts")
    public ResponseEntity<List<LatencyDataPoint>> getUptimeTrend(
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Reusing the same data structure as latency since success/failure is in it
        return ResponseEntity.ok(analyticsService.getLatencyTrend(monitorId, userDetails.getWorkspaceId(), 1));
    }
}
