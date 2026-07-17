package com.pulsewatch.backend.monitor.repository;

import com.pulsewatch.backend.monitor.entity.WorkspaceActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceActivityRepository extends JpaRepository<WorkspaceActivity, UUID> {
    List<WorkspaceActivity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
