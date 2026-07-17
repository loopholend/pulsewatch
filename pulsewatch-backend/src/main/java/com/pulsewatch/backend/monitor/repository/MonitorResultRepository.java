package com.pulsewatch.backend.monitor.repository;

import com.pulsewatch.backend.monitor.entity.MonitorResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MonitorResultRepository extends JpaRepository<MonitorResult, UUID> {

    // Efficient single-row fetch for monitor overview (replaces findTop100 + get(0))
    MonitorResult findFirstByMonitorIdOrderByCheckedAtDesc(UUID monitorId);

    // Efficient single-row fetch for last successful check
    MonitorResult findFirstByMonitorIdAndSuccessTrueOrderByCheckedAtDesc(UUID monitorId);

    // Time-windowed history for the /history?window= endpoint
    List<MonitorResult> findByMonitorIdAndCheckedAtAfterOrderByCheckedAtDesc(
            UUID monitorId, LocalDateTime since);

    @Query("SELECT COUNT(r) FROM MonitorResult r WHERE r.monitorId = :monitorId")
    long countByMonitorId(UUID monitorId);

    @Query("SELECT COUNT(r) FROM MonitorResult r WHERE r.monitorId = :monitorId AND r.success = true")
    long countSuccessByMonitorId(UUID monitorId);

    @Query("SELECT AVG(r.responseTimeMs) FROM MonitorResult r WHERE r.monitorId = :monitorId AND r.responseTimeMs IS NOT NULL")
    Double avgResponseTimeByMonitorId(UUID monitorId);

    // Rolling average for a specific time window (used by analytics)
    @Query("SELECT AVG(r.responseTimeMs) FROM MonitorResult r " +
           "WHERE r.monitorId = :monitorId AND r.checkedAt >= :since AND r.responseTimeMs IS NOT NULL")
    Double avgResponseTimeSince(@Param("monitorId") UUID monitorId, @Param("since") LocalDateTime since);

    // Bulk average for dashboard summary across multiple monitors
    @Query("SELECT AVG(r.responseTimeMs) FROM MonitorResult r " +
           "WHERE r.monitorId IN :monitorIds AND r.checkedAt >= :since AND r.responseTimeMs IS NOT NULL")
    Double avgResponseTimeSinceIn(@Param("monitorIds") List<UUID> monitorIds, @Param("since") LocalDateTime since);

    // All results for a monitor within a time window (for export)
    @Query("SELECT r FROM MonitorResult r WHERE r.monitorId = :monitorId AND r.checkedAt >= :since " +
           "ORDER BY r.checkedAt DESC")
    List<MonitorResult> findSince(@Param("monitorId") UUID monitorId, @Param("since") LocalDateTime since);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM MonitorResult r WHERE r.checkedAt < :cutoff")
    int deleteByCheckedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
