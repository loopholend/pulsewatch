package com.pulsewatch.backend.analytics.service;

import com.pulsewatch.backend.analytics.dto.DashboardSummaryResponse;
import com.pulsewatch.backend.analytics.entity.MonitorStats;
import com.pulsewatch.backend.analytics.repository.AnalyticsResultRepository;
import com.pulsewatch.backend.analytics.repository.MonitorStatsRepository;
import com.pulsewatch.backend.incident.repository.IncidentRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import com.pulsewatch.backend.monitor.repository.MonitorResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService math helpers and dashboard summary.
 * Uses workspace-scoped repository calls (post-V11 migration).
 */
@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock private AnalyticsResultRepository analyticsResultRepository;
    @Mock private MonitorStatsRepository monitorStatsRepository;
    @Mock private MonitorRepository monitorRepository;
    @Mock private MonitorResultRepository monitorResultRepository;
    @Mock private IncidentRepository incidentRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void testCalculateUptime() {
        assertEquals(100.0, analyticsService.calculateUptime(10, 10));
        assertEquals(50.0, analyticsService.calculateUptime(5, 10));
        assertEquals(0.0, analyticsService.calculateUptime(0, 0));
        assertEquals(33.33, analyticsService.calculateUptime(1, 3));
    }

    @Test
    void testCalculateAverageLatency() {
        assertEquals(0, analyticsService.calculateAverageLatency(null));
        assertEquals(0, analyticsService.calculateAverageLatency(Collections.emptyList()));
        assertEquals(150, analyticsService.calculateAverageLatency(Arrays.asList(100, 200)));
        assertEquals(133, analyticsService.calculateAverageLatency(Arrays.asList(100, 100, 200))); // 400/3 = 133.33 -> 133
    }

    @Test
    void testCalculateP95Latency() {
        assertEquals(0, analyticsService.calculateP95Latency(null));
        assertEquals(0, analyticsService.calculateP95Latency(Collections.emptyList()));

        List<Integer> latencies = Arrays.asList(
            10, 20, 30, 40, 50, 60, 70, 80, 90, 100,
            110, 120, 130, 140, 150, 160, 170, 180, 190, 200
        );
        // ceil(0.95 * 20) - 1 = ceil(19) - 1 = 18 => latencies[18] = 190
        assertEquals(190, analyticsService.calculateP95Latency(latencies));
    }

    @Test
    void testCalculateErrorRate() {
        assertEquals(0.0, analyticsService.calculateErrorRate(0, 0));
        assertEquals(20.0, analyticsService.calculateErrorRate(2, 10));
    }

    @Test
    void testGetDashboardSummary() {
        UUID workspaceId = UUID.randomUUID();

        Monitor upMonitor = new Monitor();
        upMonitor.setId(UUID.randomUUID());
        upMonitor.setCurrentStatus("UP");

        Monitor downMonitor = new Monitor();
        downMonitor.setId(UUID.randomUUID());
        downMonitor.setCurrentStatus("DOWN");

        // Post-migration: use workspace-scoped findByWorkspaceId
        when(monitorRepository.findByWorkspaceId(workspaceId)).thenReturn(Arrays.asList(upMonitor, downMonitor));

        // Post-migration: use countByWorkspaceIdAndStatus (workspace-scoped)
        when(incidentRepository.countByWorkspaceIdAndStatus(workspaceId, "OPEN")).thenReturn(3L);

        MonitorStats upStats = new MonitorStats(upMonitor.getId());
        upStats.setUptime(100.0);
        MonitorStats downStats = new MonitorStats(downMonitor.getId());
        downStats.setUptime(50.0);
        when(monitorStatsRepository.findAllById(any())).thenReturn(Arrays.asList(upStats, downStats));
        when(monitorResultRepository.avgResponseTimeSinceIn(any(), any(LocalDateTime.class))).thenReturn(150.0);

        DashboardSummaryResponse summary = analyticsService.getDashboardSummary(workspaceId);

        assertEquals(2, summary.getTotalMonitors());
        assertEquals(1, summary.getUpCount());
        assertEquals(1, summary.getDownCount());
        assertEquals(0, summary.getUnknownCount());
        assertEquals(3, summary.getOpenIncidents());
        assertEquals(75.0, summary.getAvgUptimePercent());
        assertEquals(150, summary.getAvgResponseTimeLast24Hours());
    }
}
