package com.pulsewatch.backend.alert.repository;

import com.pulsewatch.backend.alert.entity.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {
    List<AlertRule> findByMonitorIdAndEnabledTrue(UUID monitorId);
    List<AlertRule> findByMonitorIdAndRuleTypeAndEnabledTrue(UUID monitorId, String ruleType);
    List<AlertRule> findByMonitorId(UUID monitorId);

    @Query("SELECT r FROM AlertRule r JOIN Monitor m ON r.monitorId = m.id WHERE m.workspaceId = :workspaceId")
    Page<AlertRule> findByWorkspaceId(@Param("workspaceId") UUID workspaceId, Pageable pageable);
}
