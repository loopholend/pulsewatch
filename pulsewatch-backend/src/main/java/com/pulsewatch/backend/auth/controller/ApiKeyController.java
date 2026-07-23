package com.pulsewatch.backend.auth.controller;

import com.pulsewatch.backend.auth.dto.CreateApiKeyRequest;
import com.pulsewatch.backend.auth.entity.ApiKey;
import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.auth.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/api-keys")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "API Keys", description = "Personal API access tokens management APIs")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    @GetMapping
    @Operation(summary = "List all active API keys for the current workspace")
    public ResponseEntity<List<ApiKey>> getActiveKeys(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(apiKeyService.getActiveKeys(userDetails.getWorkspaceId()));
    }

    @PostMapping
    @Operation(summary = "Generate a new API key")
    public ResponseEntity<?> generateKey(
            @Valid @RequestBody CreateApiKeyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ApiKeyService.GeneratedApiKey generated = apiKeyService.generateKey(
                userDetails.getWorkspaceId(),
                userDetails.getId(),
                request.getName(),
                request.getDurationDays()
        );
        return ResponseEntity.ok(Map.of(
                "id", generated.apiKey.getId(),
                "name", generated.apiKey.getName(),
                "maskedKey", generated.apiKey.getMaskedKey(),
                "rawKey", generated.rawKey,
                "createdAt", generated.apiKey.getCreatedAt(),
                "expiresAt", generated.apiKey.getExpiresAt() != null ? generated.apiKey.getExpiresAt() : ""
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke / delete an API key")
    public ResponseEntity<?> revokeKey(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        apiKeyService.revokeKey(id, userDetails.getWorkspaceId());
        return ResponseEntity.ok(Map.of("message", "API key revoked successfully"));
    }
}
