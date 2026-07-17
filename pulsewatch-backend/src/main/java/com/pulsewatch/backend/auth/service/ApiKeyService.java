package com.pulsewatch.backend.auth.service;

import com.pulsewatch.backend.auth.entity.ApiKey;
import com.pulsewatch.backend.auth.repository.ApiKeyRepository;
import com.pulsewatch.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public static class GeneratedApiKey {
        public final ApiKey apiKey;
        public final String rawKey;

        public GeneratedApiKey(ApiKey apiKey, String rawKey) {
            this.apiKey = apiKey;
            this.rawKey = rawKey;
        }
    }

    public List<ApiKey> getActiveKeys(UUID workspaceId) {
        return apiKeyRepository.findByWorkspaceIdAndActiveTrueOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional
    public GeneratedApiKey generateKey(UUID workspaceId, String name, int durationDays) {
        // Generate secure 32-byte hex token
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        String hexToken = HexFormat.of().formatHex(bytes);
        String rawKey = "pw_live_" + hexToken;

        String keyHash = hashKey(rawKey);
        String maskedKey = "pw_live_" + hexToken.substring(0, 4) + "..." + hexToken.substring(hexToken.length() - 4);

        LocalDateTime expiresAt = durationDays > 0 ? LocalDateTime.now().plusDays(durationDays) : null;

        ApiKey apiKey = new ApiKey(workspaceId, name, keyHash, maskedKey, expiresAt);
        ApiKey saved = apiKeyRepository.save(apiKey);

        return new GeneratedApiKey(saved, rawKey);
    }

    @Transactional
    public void revokeKey(UUID id, UUID workspaceId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("API Key not found in active workspace"));
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
    }

    public Optional<ApiKey> validateKey(String rawKey) {
        String keyHash = hashKey(rawKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHashAndActiveTrue(keyHash);
        
        if (apiKeyOpt.isPresent()) {
            ApiKey key = apiKeyOpt.get();
            if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
                // key has expired - set it inactive
                key.setActive(false);
                apiKeyRepository.save(key);
                return Optional.empty();
            }
            return apiKeyOpt;
        }
        return Optional.empty();
    }

    public String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing not available", e);
        }
    }
}
