package com.pulsewatch.backend.maintenance.controller;

import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.maintenance.dto.MaintenanceWindowRequest;
import com.pulsewatch.backend.maintenance.entity.MaintenanceWindow;
import com.pulsewatch.backend.maintenance.service.MaintenanceWindowService;
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
@RequestMapping("/api/monitors")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Maintenance Windows", description = "Planned downtime management")
public class MaintenanceWindowController {

    @Autowired
    private MaintenanceWindowService maintenanceWindowService;

    @PostMapping("/{monitorId}/maintenance")
    @Operation(summary = "Schedule a maintenance window for a monitor")
    public ResponseEntity<MaintenanceWindow> create(
            @PathVariable UUID monitorId,
            @RequestBody MaintenanceWindowRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(maintenanceWindowService.create(monitorId, request, userDetails.getWorkspaceId()));
    }

    @GetMapping("/{monitorId}/maintenance")
    @Operation(summary = "List all maintenance windows for a monitor")
    public ResponseEntity<List<MaintenanceWindow>> list(
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(maintenanceWindowService.listByMonitor(monitorId, userDetails.getWorkspaceId()));
    }

    @DeleteMapping("/maintenance/{windowId}")
    @Operation(summary = "Delete a maintenance window")
    public ResponseEntity<Void> delete(
            @PathVariable UUID windowId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        maintenanceWindowService.delete(windowId, userDetails.getWorkspaceId());
        return ResponseEntity.noContent().build();
    }
}
