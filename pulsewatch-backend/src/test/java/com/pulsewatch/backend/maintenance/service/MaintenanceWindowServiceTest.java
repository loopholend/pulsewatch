package com.pulsewatch.backend.maintenance.service;

import com.pulsewatch.backend.exception.ResourceNotFoundException;
import com.pulsewatch.backend.maintenance.dto.MaintenanceWindowRequest;
import com.pulsewatch.backend.maintenance.entity.MaintenanceWindow;
import com.pulsewatch.backend.maintenance.repository.MaintenanceWindowRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MaintenanceWindowService.
 * Uses workspace-scoped repository calls (post-V11 migration).
 * - monitor.setUserId() → monitor.setWorkspaceId()
 * - monitorRepository.findById() → monitorRepository.findByIdAndWorkspaceId()
 */
@ExtendWith(MockitoExtension.class)
public class MaintenanceWindowServiceTest {

    @Mock private MaintenanceWindowRepository maintenanceWindowRepository;
    @Mock private MonitorRepository monitorRepository;

    @InjectMocks
    private MaintenanceWindowService maintenanceWindowService;

    @Test
    void testCreate_success() {
        UUID workspaceId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();

        MaintenanceWindowRequest request = new MaintenanceWindowRequest();
        request.setStartsAt(LocalDateTime.now().plusDays(1));
        request.setEndsAt(LocalDateTime.now().plusDays(2));
        request.setReason("Upgrade");

        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        monitor.setWorkspaceId(workspaceId);

        when(monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)).thenReturn(Optional.of(monitor));
        when(maintenanceWindowRepository.save(any(MaintenanceWindow.class))).thenAnswer(i -> i.getArguments()[0]);

        MaintenanceWindow saved = maintenanceWindowService.create(monitorId, request, workspaceId);

        assertNotNull(saved);
        assertEquals(monitorId, saved.getMonitorId());
        assertEquals("Upgrade", saved.getReason());
    }

    @Test
    void testCreate_invalidDates() {
        UUID workspaceId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();

        MaintenanceWindowRequest request = new MaintenanceWindowRequest();
        request.setStartsAt(LocalDateTime.now().plusDays(2));
        request.setEndsAt(LocalDateTime.now().plusDays(1)); // ends before starts

        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        monitor.setWorkspaceId(workspaceId);

        when(monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)).thenReturn(Optional.of(monitor));

        assertThrows(IllegalArgumentException.class, () -> {
            maintenanceWindowService.create(monitorId, request, workspaceId);
        });
    }

    @Test
    void testCreate_wrongWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();

        MaintenanceWindowRequest request = new MaintenanceWindowRequest();
        request.setStartsAt(LocalDateTime.now().plusDays(1));
        request.setEndsAt(LocalDateTime.now().plusDays(2));

        // Monitor belongs to a different workspace — findByIdAndWorkspaceId returns empty
        when(monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            maintenanceWindowService.create(monitorId, request, workspaceId);
        });
    }

    @Test
    void testListByMonitor_success() {
        UUID workspaceId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();

        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        monitor.setWorkspaceId(workspaceId);

        when(monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)).thenReturn(Optional.of(monitor));
        when(maintenanceWindowRepository.findByMonitorIdOrderByStartsAtDesc(monitorId))
                .thenReturn(Collections.singletonList(new MaintenanceWindow()));

        List<MaintenanceWindow> list = maintenanceWindowService.listByMonitor(monitorId, workspaceId);
        assertEquals(1, list.size());
    }

    @Test
    void testDelete_success() {
        UUID workspaceId = UUID.randomUUID();
        UUID windowId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();

        MaintenanceWindow window = new MaintenanceWindow();
        ReflectionTestUtils.setField(window, "id", windowId);
        window.setMonitorId(monitorId);

        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        monitor.setWorkspaceId(workspaceId);

        when(maintenanceWindowRepository.findById(windowId)).thenReturn(Optional.of(window));
        when(monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)).thenReturn(Optional.of(monitor));

        maintenanceWindowService.delete(windowId, workspaceId);

        verify(maintenanceWindowRepository).delete(window);
    }

    @Test
    void testIsInMaintenance() {
        UUID monitorId = UUID.randomUUID();

        when(maintenanceWindowRepository.findActiveWindow(eq(monitorId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new MaintenanceWindow()));

        assertTrue(maintenanceWindowService.isInMaintenance(monitorId));

        when(maintenanceWindowRepository.findActiveWindow(eq(monitorId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertFalse(maintenanceWindowService.isInMaintenance(monitorId));
    }
}
