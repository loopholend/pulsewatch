package com.pulsewatch.backend.statuspage.controller;

import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.statuspage.dto.CreateStatusPageRequest;
import com.pulsewatch.backend.statuspage.dto.PublicStatusPageResponse;
import com.pulsewatch.backend.statuspage.entity.ServiceStatus;
import com.pulsewatch.backend.statuspage.entity.StatusPage;
import com.pulsewatch.backend.statuspage.service.StatusPageService;
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
@Tag(name = "Status Pages", description = "Status Page Management APIs")
public class StatusPageController {

    @Autowired
    private StatusPageService statusPageService;

    // ── Authenticated endpoints ──────────────────────────────────────────────

    @PostMapping("/api/status-pages")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new status page")
    public ResponseEntity<StatusPage> create(
            @RequestBody CreateStatusPageRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(statusPageService.create(request, userDetails.getWorkspaceId()));
    }

    @PutMapping("/api/status-pages/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a status page")
    public ResponseEntity<StatusPage> update(
            @PathVariable UUID id,
            @RequestBody CreateStatusPageRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(statusPageService.update(id, request, userDetails.getWorkspaceId()));
    }

    @DeleteMapping("/api/status-pages/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a status page")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        statusPageService.delete(id, userDetails.getWorkspaceId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/status-pages")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List all status pages for the current workspace")
    public ResponseEntity<List<StatusPage>> list(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(statusPageService.listByUser(userDetails.getWorkspaceId()));
    }

    @PostMapping("/api/status-pages/{id}/services/{monitorId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a monitor as a service to a status page")
    public ResponseEntity<ServiceStatus> addService(
            @PathVariable UUID id,
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(statusPageService.addService(id, monitorId, userDetails.getWorkspaceId()));
    }

    @DeleteMapping("/api/status-pages/{id}/services/{monitorId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Remove a monitor from a status page")
    public ResponseEntity<Void> removeService(
            @PathVariable UUID id,
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        statusPageService.removeService(id, monitorId, userDetails.getWorkspaceId());
        return ResponseEntity.noContent().build();
    }

    // ── Public endpoints (no auth) ───────────────────────────────────────────

    @GetMapping("/public/status/{slug}")
    @Operation(summary = "Get public status page by slug (no authentication required)")
    public ResponseEntity<PublicStatusPageResponse> getPublicPage(@PathVariable String slug) {
        return ResponseEntity.ok(statusPageService.getPublicPage(slug));
    }
}
