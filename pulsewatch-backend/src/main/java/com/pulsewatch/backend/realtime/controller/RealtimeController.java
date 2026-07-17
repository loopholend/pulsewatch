package com.pulsewatch.backend.realtime.controller;

import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.realtime.service.RealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/realtime")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Realtime", description = "Realtime SSE Streaming APIs")
public class RealtimeController {

    @Autowired
    private RealtimeService realtimeService;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Establish real-time Server-Sent Events connection")
    public SseEmitter connect(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Register emitter keyed by workspaceId so events broadcast by IncidentService
        // and MonitorExecutionService (which key by workspaceId) reach connected clients.
        return realtimeService.createEmitter(userDetails.getWorkspaceId());
    }
}
