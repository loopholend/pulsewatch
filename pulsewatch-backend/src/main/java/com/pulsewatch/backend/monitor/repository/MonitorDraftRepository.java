package com.pulsewatch.backend.monitor.repository;

import com.pulsewatch.backend.monitor.entity.MonitorDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitorDraftRepository extends JpaRepository<MonitorDraft, UUID> {
    Optional<MonitorDraft> findByMonitorId(UUID monitorId);
    Optional<MonitorDraft> findByMonitorIdAndWorkspaceId(UUID monitorId, UUID workspaceId);
    void deleteByMonitorId(UUID monitorId);
}
