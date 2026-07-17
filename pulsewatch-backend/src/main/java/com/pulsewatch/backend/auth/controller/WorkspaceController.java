package com.pulsewatch.backend.auth.controller;

import com.pulsewatch.backend.auth.entity.Workspace;
import com.pulsewatch.backend.auth.entity.WorkspaceMember;
import com.pulsewatch.backend.auth.repository.WorkspaceRepository;
import com.pulsewatch.backend.auth.repository.WorkspaceMemberRepository;
import com.pulsewatch.backend.auth.security.jwt.JwtUtils;
import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workspaces")
@Tag(name = "Workspace", description = "Workspace management and switching APIs")
public class WorkspaceController {

    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private com.pulsewatch.backend.monitor.repository.WorkspaceActivityRepository workspaceActivityRepository;

    @GetMapping("/activity")
    @Operation(summary = "Get activity feed for the active workspace")
    public ResponseEntity<?> getWorkspaceActivity(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails.getWorkspaceId() == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(workspaceActivityRepository.findByWorkspaceIdOrderByCreatedAtDesc(userDetails.getWorkspaceId()));
    }

    @GetMapping
    @Operation(summary = "Get all workspaces for the authenticated user")
    public ResponseEntity<?> getUserWorkspaces() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(userDetails.getId());
        List<Map<String, Object>> workspacesList = memberships.stream().map(member -> {
            Workspace workspace = workspaceRepository.findById(member.getWorkspaceId()).orElse(null);
            if (workspace == null) return null;
            Map<String, Object> map = new HashMap<>();
            map.put("id", workspace.getId());
            map.put("name", workspace.getName());
            map.put("slug", workspace.getSlug());
            map.put("role", member.getRole());
            map.put("joinedAt", member.getJoinedAt());
            return map;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return ResponseEntity.ok(workspacesList);
    }

    @PostMapping("/{id}/switch")
    @Operation(summary = "Switch the active workspace and generate a new JWT")
    public ResponseEntity<?> switchWorkspace(@PathVariable("id") UUID workspaceId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Optional<WorkspaceMember> memberOpt = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userDetails.getId());

        if (memberOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You do not have access to this workspace"));
        }

        WorkspaceMember member = memberOpt.get();
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();

        // Generate a new token with updated claims
        String newToken = jwtUtils.generateTokenFromUsername(userDetails.getUsername(), workspace.getId(), member.getRole());

        return ResponseEntity.ok(Map.of(
                "accessToken", newToken,
                "tokenType", "Bearer",
                "workspaceId", workspace.getId(),
                "workspaceRole", member.getRole(),
                "workspaceName", workspace.getName()
        ));
    }
}
