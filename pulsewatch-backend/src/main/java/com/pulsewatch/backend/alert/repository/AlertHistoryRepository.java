package com.pulsewatch.backend.alert.repository;

import com.pulsewatch.backend.alert.entity.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, UUID> {
    List<AlertHistory> findByMonitorIdOrderBySentAtDesc(UUID monitorId);
    List<AlertHistory> findAllByOrderBySentAtDesc();

    @Query("SELECT h FROM AlertHistory h JOIN Monitor m ON h.monitorId = m.id WHERE " +
           "m.workspaceId = :workspaceId AND " +
           "(:status IS NULL OR h.status = :status) " +
           "ORDER BY h.sentAt DESC")
    Page<AlertHistory> findByWorkspaceIdAndFilters(
            @Param("workspaceId") UUID workspaceId, 
            @Param("status") String status, 
            Pageable pageable);
}
