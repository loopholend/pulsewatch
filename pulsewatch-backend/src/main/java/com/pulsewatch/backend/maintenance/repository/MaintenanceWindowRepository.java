package com.pulsewatch.backend.maintenance.repository;

import com.pulsewatch.backend.maintenance.entity.MaintenanceWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, UUID> {

    List<MaintenanceWindow> findByMonitorIdOrderByStartsAtDesc(UUID monitorId);

    /**
     * Returns an active maintenance window for a monitor at the given time.
     * A window is active if starts_at <= now <= ends_at.
     */
    @Query("SELECT mw FROM MaintenanceWindow mw WHERE mw.monitorId = :monitorId " +
           "AND mw.startsAt <= :now AND mw.endsAt >= :now")
    Optional<MaintenanceWindow> findActiveWindow(
            @Param("monitorId") UUID monitorId,
            @Param("now") LocalDateTime now);
}
