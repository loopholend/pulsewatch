package com.pulsewatch.backend.assertion.repository;

import com.pulsewatch.backend.assertion.entity.MonitorAssertion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonitorAssertionRepository extends JpaRepository<MonitorAssertion, UUID> {
    List<MonitorAssertion> findByMonitorIdOrderByCreatedAtAsc(UUID monitorId);
    void deleteAllByMonitorId(UUID monitorId);
}
