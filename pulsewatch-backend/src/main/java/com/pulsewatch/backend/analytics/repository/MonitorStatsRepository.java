package com.pulsewatch.backend.analytics.repository;

import com.pulsewatch.backend.analytics.entity.MonitorStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MonitorStatsRepository extends JpaRepository<MonitorStats, UUID> {
}
