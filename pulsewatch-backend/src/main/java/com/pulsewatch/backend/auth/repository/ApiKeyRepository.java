package com.pulsewatch.backend.auth.repository;

import com.pulsewatch.backend.auth.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByWorkspaceIdAndActiveTrueOrderByCreatedAtDesc(UUID workspaceId);
    Optional<ApiKey> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);
}
