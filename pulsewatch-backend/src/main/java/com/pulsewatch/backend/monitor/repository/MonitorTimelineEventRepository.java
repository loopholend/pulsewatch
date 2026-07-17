package com.pulsewatch.backend.monitor.repository;

import com.pulsewatch.backend.monitor.entity.MonitorTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonitorTimelineEventRepository extends JpaRepository<MonitorTimelineEvent, UUID> {
    List<MonitorTimelineEvent> findByMonitorIdAndWorkspaceIdOrderByCreatedAtDesc(UUID monitorId, UUID workspaceId);
}
