package com.pulsewatch.backend.incident.repository;

import com.pulsewatch.backend.incident.entity.Incident;
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
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    Optional<Incident> findFirstByMonitorIdAndStatusOrderByStartedAtDesc(UUID monitorId, String status);
    Optional<Incident> findFirstByMonitorIdAndStatusInOrderByStartedAtDesc(UUID monitorId, List<String> statuses);
    Optional<Incident> findFirstByMonitorIdOrderByStartedAtDesc(UUID monitorId);
    List<Incident> findByMonitorIdOrderByStartedAtDesc(UUID monitorId);

    /**
     * Paginated incident listing with optional status and severity filters.
     */
    @Query("SELECT i FROM Incident i WHERE " +
           "(:status IS NULL OR i.status = :status) " +
           "AND (:severity IS NULL OR i.severity = :severity) " +
           "ORDER BY i.startedAt DESC")
    Page<Incident> findByFilters(
            @Param("status")   String status,
            @Param("severity") String severity,
            Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE " +
           "i.workspaceId = :workspaceId AND " +
           "(:status IS NULL OR i.status = :status) " +
           "AND (:severity IS NULL OR i.severity = :severity) " +
           "ORDER BY i.startedAt DESC")
    Page<Incident> findByWorkspaceIdAndFilters(
            @Param("workspaceId") UUID workspaceId,
            @Param("status")   String status,
            @Param("severity") String severity,
            Pageable pageable);

    long countByWorkspaceIdAndStatus(UUID workspaceId, String status);

    List<Incident> findByWorkspaceId(UUID workspaceId);

    Optional<Incident> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
