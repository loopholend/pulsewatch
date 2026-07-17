package com.pulsewatch.backend.analytics.repository;

import com.pulsewatch.backend.monitor.entity.MonitorResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsResultRepository extends JpaRepository<MonitorResult, UUID> {

    @Query("SELECT r FROM MonitorResult r WHERE r.monitorId = :monitorId AND r.checkedAt >= :since ORDER BY r.responseTimeMs ASC")
    List<MonitorResult> findResultsSince(UUID monitorId, LocalDateTime since);

    @Query("SELECT COUNT(r) FROM MonitorResult r WHERE r.monitorId = :monitorId AND r.checkedAt >= :since")
    long countSince(UUID monitorId, LocalDateTime since);

    @Query("SELECT COUNT(r) FROM MonitorResult r WHERE r.monitorId = :monitorId AND r.success = true AND r.checkedAt >= :since")
    long countSuccessSince(UUID monitorId, LocalDateTime since);

    @Query("SELECT r FROM MonitorResult r WHERE r.monitorId = :monitorId AND r.checkedAt >= :since ORDER BY r.checkedAt DESC")
    List<MonitorResult> findRecentResults(UUID monitorId, LocalDateTime since);
}
