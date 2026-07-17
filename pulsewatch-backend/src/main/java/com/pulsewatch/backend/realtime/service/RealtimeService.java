package com.pulsewatch.backend.realtime.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeService.class);

    // Map of Workspace ID -> List of active SseEmitters
    // Events are broadcast per-workspace so every connected member receives updates.
    private final Map<UUID, List<SseEmitter>> workspaceEmitters = new ConcurrentHashMap<>();

    /**
     * Create and register a new SseEmitter for the given workspace.
     * All members of the workspace receive the same broadcast.
     */
    public SseEmitter createEmitter(UUID workspaceId) {
        SseEmitter emitter = new SseEmitter(1_800_000L); // 30-minute timeout

        List<SseEmitter> emitters = workspaceEmitters.computeIfAbsent(workspaceId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(workspaceId, emitter));
        emitter.onTimeout(() -> removeEmitter(workspaceId, emitter));
        emitter.onError((e) -> removeEmitter(workspaceId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("Live connection established"));
        } catch (IOException e) {
            removeEmitter(workspaceId, emitter);
        }

        logger.info("SSE emitter created for workspace {}. Active emitters: {}", workspaceId, emitters.size());
        return emitter;
    }

    /**
     * Send a real-time event to all connected clients in the given workspace.
     */
    public void sendEvent(UUID workspaceId, String eventType, Object payload) {
        if (workspaceId == null) return;

        List<SseEmitter> emitters = workspaceEmitters.get(workspaceId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        logger.debug("Broadcasting '{}' to workspace {} ({} emitters)", eventType, workspaceId, emitters.size());
        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(payload));
            } catch (Exception e) {
                logger.warn("Dead SSE emitter found in workspace {}, removing.", workspaceId);
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                workspaceEmitters.remove(workspaceId);
            }
        }
    }

    /**
     * Remove a specific emitter from the active list.
     */
    private void removeEmitter(UUID workspaceId, SseEmitter emitter) {
        List<SseEmitter> emitters = workspaceEmitters.get(workspaceId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                workspaceEmitters.remove(workspaceId);
            }
            logger.info("Removed SSE emitter for workspace {}. Remaining: {}", workspaceId, emitters.size());
        }
    }
}
