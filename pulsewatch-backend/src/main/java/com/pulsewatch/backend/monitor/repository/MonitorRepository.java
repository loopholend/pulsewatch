package com.pulsewatch.backend.monitor.repository;

import com.pulsewatch.backend.monitor.entity.Monitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, UUID> {
    List<Monitor> findByWorkspaceId(UUID workspaceId);
    List<Monitor> findByActiveTrue();
    List<Monitor> findByWorkspaceIdAndActiveTrue(UUID workspaceId);
    Optional<Monitor> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    List<Monitor> findByWorkspaceIdAndUrlAndActiveTrue(UUID workspaceId, String url);

    /**
     * Paginated search with optional status and name filters.
     * Passing null for status or search disables that filter.
     */
    @Query("SELECT m FROM Monitor m WHERE m.workspaceId = :workspaceId " +
           "AND (:status IS NULL OR m.currentStatus = :status) " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE :search)")
    Page<Monitor> findByFilters(
            @Param("workspaceId")  UUID workspaceId,
            @Param("status")  String status,
            @Param("search")  String search,
            Pageable pageable);

    // Used for dashboard summary counts
    long countByWorkspaceIdAndCurrentStatus(UUID workspaceId, String currentStatus);
    long countByWorkspaceId(UUID workspaceId);
}
