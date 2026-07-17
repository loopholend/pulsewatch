package com.pulsewatch.backend.incident.controller;

import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.exception.ResourceNotFoundException;
import com.pulsewatch.backend.incident.dto.IncidentResponse;
import com.pulsewatch.backend.incident.entity.Incident;
import com.pulsewatch.backend.incident.entity.IncidentEvent;
import com.pulsewatch.backend.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Incidents", description = "Incident Management APIs")
public class IncidentController {

    @Autowired private IncidentService incidentService;
    @Autowired private com.pulsewatch.backend.monitor.repository.MonitorRepository monitorRepository;

    @GetMapping
    @Operation(summary = "List incidents with optional status/severity filters and pagination")
    public ResponseEntity<Page<IncidentResponse>> getAllIncidents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        Page<IncidentResponse> result = incidentService
                .getAllIncidents(userDetails.getWorkspaceId(), status, severity, pageable)
                .map(this::mapToResponse);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/monitor/{monitorId}")
    @Operation(summary = "Get all incidents for a specific monitor")
    public ResponseEntity<List<IncidentResponse>> getIncidentsByMonitor(
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(
                incidentService.getIncidentsByMonitor(monitorId, userDetails.getWorkspaceId()).stream()
                        .map(this::mapToResponse)
                        .toList()
        );
    }

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get detailed information for a single incident")
    public ResponseEntity<IncidentResponse> getIncident(
         @PathVariable UUID incidentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Incident incident = incidentService.getIncidentById(incidentId, userDetails.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
        return ResponseEntity.ok(mapToResponse(incident));
    }

    @GetMapping("/{incidentId}/events")
    @Operation(summary = "Get the event timeline for an incident")
    public ResponseEntity<List<IncidentEvent>> getIncidentEvents(
            @PathVariable UUID incidentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(incidentService.getEventsByIncident(incidentId, userDetails.getWorkspaceId()));
    }

    @PatchMapping("/{incidentId}/acknowledge")
    @Operation(summary = "Acknowledge an open incident")
    public ResponseEntity<IncidentResponse> acknowledgeIncident(
            @PathVariable UUID incidentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Incident incident = incidentService.acknowledgeIncident(incidentId, userDetails.getWorkspaceId());
        return ResponseEntity.ok(mapToResponse(incident));
    }

    @PatchMapping("/{incidentId}/resolve")
    @Operation(summary = "Manually resolve an incident")
    public ResponseEntity<IncidentResponse> resolveIncident(
            @PathVariable UUID incidentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Incident incident = incidentService.resolveIncident(incidentId, userDetails.getWorkspaceId());
        return ResponseEntity.ok(mapToResponse(incident));
    }

    private IncidentResponse mapToResponse(Incident incident) {
        IncidentResponse response = new IncidentResponse();
        response.setId(incident.getId());
        response.setMonitorId(incident.getMonitorId());
        response.setStatus(incident.getStatus());
        response.setSeverity(incident.getSeverity());
        response.setStartedAt(incident.getStartedAt());
        response.setResolvedAt(incident.getResolvedAt());
        response.setDurationSeconds(incident.getDurationSeconds());
        response.setFailureReason(incident.getFailureReason());
        
        // Map monitor name
        java.util.Optional<com.pulsewatch.backend.monitor.entity.Monitor> mOpt = monitorRepository.findById(incident.getMonitorId());
        if (mOpt.isPresent()) {
            response.setMonitorName(mOpt.get().getName());
        } else {
            response.setMonitorName("Deleted Monitor (" + incident.getMonitorId().toString().substring(0, 8) + ")");
        }
        
        return response;
    }
}
