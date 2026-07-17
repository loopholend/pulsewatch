package com.pulsewatch.backend.statuspage.repository;

import com.pulsewatch.backend.statuspage.entity.StatusPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusPageRepository extends JpaRepository<StatusPage, UUID> {
    List<StatusPage> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    Optional<StatusPage> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    Optional<StatusPage> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
