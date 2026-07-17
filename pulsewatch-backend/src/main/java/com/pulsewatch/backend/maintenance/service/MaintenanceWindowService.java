package com.pulsewatch.backend.maintenance.service;

import com.pulsewatch.backend.exception.ResourceNotFoundException;
import com.pulsewatch.backend.maintenance.dto.MaintenanceWindowRequest;
import com.pulsewatch.backend.maintenance.entity.MaintenanceWindow;
import com.pulsewatch.backend.maintenance.repository.MaintenanceWindowRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MaintenanceWindowService {

    @Autowired private MaintenanceWindowRepository maintenanceWindowRepository;
    @Autowired private MonitorRepository monitorRepository;

    @Transactional
    public MaintenanceWindow create(UUID monitorId, MaintenanceWindowRequest request, UUID workspaceId) {
        // Verify monitor ownership
        Monitor monitor = monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found in active workspace"));

        if (request.getEndsAt() == null || request.getStartsAt() == null) {
            throw new IllegalArgumentException("startsAt and endsAt are required");
        }
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }

        MaintenanceWindow window = new MaintenanceWindow();
        window.setMonitorId(monitorId);
        window.setWorkspaceId(workspaceId);
        window.setStartsAt(request.getStartsAt());
        window.setEndsAt(request.getEndsAt());
        window.setReason(request.getReason());
        return maintenanceWindowRepository.save(window);
    }

    public List<MaintenanceWindow> listByMonitor(UUID monitorId, UUID workspaceId) {
        Monitor monitor = monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found in active workspace"));
        return maintenanceWindowRepository.findByMonitorIdOrderByStartsAtDesc(monitorId);
    }

    @Transactional
    public void delete(UUID windowId, UUID workspaceId) {
        MaintenanceWindow window = maintenanceWindowRepository.findById(windowId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance window not found: " + windowId));
        // Verify ownership via monitor
        Monitor monitor = monitorRepository.findByIdAndWorkspaceId(window.getMonitorId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance window not found in active workspace"));
        maintenanceWindowRepository.delete(window);
    }

    /**
     * Called by the scheduler before processing a check.
     * Returns true if the monitor is currently in an active maintenance window.
     */
    public boolean isInMaintenance(UUID monitorId) {
        return maintenanceWindowRepository.findActiveWindow(monitorId, LocalDateTime.now()).isPresent();
    }
}
