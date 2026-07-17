package com.pulsewatch.backend.statuspage.repository;

import com.pulsewatch.backend.statuspage.entity.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceStatusRepository extends JpaRepository<ServiceStatus, UUID> {
    List<ServiceStatus> findByStatusPageIdOrderByServiceNameAsc(UUID statusPageId);
    Optional<ServiceStatus> findByStatusPageIdAndMonitorId(UUID statusPageId, UUID monitorId);
    List<ServiceStatus> findByMonitorId(UUID monitorId);
}
