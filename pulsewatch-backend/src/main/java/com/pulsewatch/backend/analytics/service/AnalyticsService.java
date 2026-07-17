package com.pulsewatch.backend.analytics.service;

import com.pulsewatch.backend.analytics.dto.DashboardSummaryResponse;
import com.pulsewatch.backend.analytics.dto.LatencyDataPoint;
import com.pulsewatch.backend.analytics.dto.MonitorStatsResponse;
import com.pulsewatch.backend.analytics.entity.MonitorStats;
import com.pulsewatch.backend.analytics.repository.AnalyticsResultRepository;
import com.pulsewatch.backend.analytics.repository.MonitorStatsRepository;
import com.pulsewatch.backend.exception.ResourceNotFoundException;
import com.pulsewatch.backend.incident.repository.IncidentRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.entity.MonitorResult;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import com.pulsewatch.backend.monitor.repository.MonitorResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired private AnalyticsResultRepository analyticsResultRepository;
    @Autowired private MonitorStatsRepository monitorStatsRepository;
    @Autowired private MonitorRepository monitorRepository;
    @Autowired private MonitorResultRepository monitorResultRepository;
    @Autowired private IncidentRepository incidentRepository;

    /**
     * Recomputes stats for a monitor and stores them in monitor_stats.
     * Called by scheduler after each check.
     */
    public void refreshMonitorStats(UUID monitorId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        // Use aggregate queries — no full result set materialized
        long totalChecks   = analyticsResultRepository.countSince(monitorId, since);
        long successChecks = analyticsResultRepository.countSuccessSince(monitorId, since);
        long failedChecks  = totalChecks - successChecks;

        double uptime    = calculateUptime(successChecks, totalChecks);
        double errorRate = calculateErrorRate(failedChecks, totalChecks);

        // AVG computed in DB, not in JVM
        Double avgLatencyDouble = monitorResultRepository.avgResponseTimeSince(monitorId, since);
        int avgLatency = avgLatencyDouble != null ? (int) Math.round(avgLatencyDouble) : 0;

        // P95 requires ordering — load only recent results for statistical calculation.
        // This is bounded by the 30-day window but computed less frequently than AVG.
        List<MonitorResult> recentForP95 = analyticsResultRepository.findRecentResults(monitorId, since);
        List<Integer> latencies = recentForP95.stream()
                .filter(r -> r.getResponseTimeMs() != null)
                .map(MonitorResult::getResponseTimeMs)
                .collect(Collectors.toList());
        int p95Latency = calculateP95Latency(latencies);

        MonitorStats stats = monitorStatsRepository.findById(monitorId)
                .orElse(new MonitorStats(monitorId));

        stats.setUptime(uptime);
        stats.setAvgLatency(avgLatency);
        stats.setP95Latency(p95Latency);
        stats.setErrorRate(errorRate);
        stats.setTotalChecks(totalChecks);
        stats.setSuccessfulChecks(successChecks);
        stats.setFailedChecks(failedChecks);

        monitorStatsRepository.save(stats);
    }
    
    public double calculateUptime(long successfulChecks, long totalChecks) {
        if (totalChecks == 0) return 0.0;
        return round2((successfulChecks * 100.0) / totalChecks);
    }
    
    public int calculateAverageLatency(List<Integer> latencies) {
        if (latencies == null || latencies.isEmpty()) return 0;
        double avg = latencies.stream().mapToInt(i -> i).average().orElse(0);
        return (int) Math.round(avg);
    }
    
    public int calculateP95Latency(List<Integer> latencies) {
        if (latencies == null || latencies.isEmpty()) return 0;
        List<Integer> sorted = new ArrayList<>(latencies);
        sorted.sort(Integer::compareTo);
        
        int index = (int) Math.ceil(0.95 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }
    
    public double calculateErrorRate(long failedChecks, long totalChecks) {
        if (totalChecks == 0) return 0.0;
        return round2((failedChecks * 100.0) / totalChecks);
    }

    /**
     * Get precomputed stats from the cache table for the dashboard.
     */
    public MonitorStatsResponse getStats(UUID monitorId, UUID workspaceId) {
        Monitor monitor = monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found in active workspace"));

        MonitorStats stats = monitorStatsRepository.findById(monitorId)
                .orElse(new MonitorStats(monitorId));

        MonitorStatsResponse response = new MonitorStatsResponse();
        response.setMonitorId(monitorId);
        response.setMonitorName(monitor.getName());
        response.setCurrentStatus(monitor.getCurrentStatus());
        response.setUptime(stats.getUptime() != null ? stats.getUptime() : 0.0);
        response.setAvgLatency(stats.getAvgLatency() != null ? stats.getAvgLatency() : 0);
        response.setP95Latency(stats.getP95Latency() != null ? stats.getP95Latency() : 0);
        response.setErrorRate(stats.getErrorRate() != null ? stats.getErrorRate() : 0.0);
        response.setTotalChecks(stats.getTotalChecks() != null ? stats.getTotalChecks() : 0L);
        response.setSuccessChecks(stats.getSuccessfulChecks() != null ? stats.getSuccessfulChecks() : 0L);
        response.setUpdatedAt(stats.getUpdatedAt());
        return response;
    }

    /**
     * Get latency trend data for a time window (24h, 7d, 30d).
     */
    public List<LatencyDataPoint> getLatencyTrend(UUID monitorId, UUID workspaceId, int days) {
        Monitor monitor = monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found in active workspace"));

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<MonitorResult> results = analyticsResultRepository.findRecentResults(monitorId, since);

        return results.stream()
                .map(r -> new LatencyDataPoint(r.getCheckedAt(), r.getResponseTimeMs(), r.isSuccess()))
                .collect(Collectors.toList());
    }

    /**
     * Get aggregated stats for all monitors belonging to a workspace (for dashboard overview).
     */
    public List<MonitorStatsResponse> getAllStats(UUID workspaceId) {
        List<Monitor> monitors = monitorRepository.findByWorkspaceId(workspaceId);
        if (monitors.isEmpty()) {
            return new ArrayList<>();
        }

        List<UUID> monitorIds = monitors.stream().map(Monitor::getId).collect(Collectors.toList());
        List<MonitorStats> allStats = monitorStatsRepository.findAllById(monitorIds);
        java.util.Map<UUID, MonitorStats> statsMap = allStats.stream()
                .collect(Collectors.toMap(MonitorStats::getMonitorId, s -> s));

        List<MonitorStatsResponse> responses = new ArrayList<>();
        for (Monitor monitor : monitors) {
            MonitorStats stats = statsMap.getOrDefault(monitor.getId(), new MonitorStats(monitor.getId()));
            MonitorStatsResponse response = new MonitorStatsResponse();
            response.setMonitorId(monitor.getId());
            response.setMonitorName(monitor.getName());
            response.setCurrentStatus(monitor.getCurrentStatus());
            response.setUptime(stats.getUptime() != null ? stats.getUptime() : 0.0);
            response.setAvgLatency(stats.getAvgLatency() != null ? stats.getAvgLatency() : 0);
            response.setP95Latency(stats.getP95Latency() != null ? stats.getP95Latency() : 0);
            response.setErrorRate(stats.getErrorRate() != null ? stats.getErrorRate() : 0.0);
            response.setTotalChecks(stats.getTotalChecks() != null ? stats.getTotalChecks() : 0L);
            response.setSuccessChecks(stats.getSuccessfulChecks() != null ? stats.getSuccessfulChecks() : 0L);
            response.setUpdatedAt(stats.getUpdatedAt());
            responses.add(response);
        }
        return responses;
    }

    /**
     * Aggregated dashboard summary for all of a workspace's monitors.
     * Includes rolling 24-hour average response time.
     */
    public DashboardSummaryResponse getDashboardSummary(UUID workspaceId) {
        List<Monitor> monitors = monitorRepository.findByWorkspaceId(workspaceId);

        long total       = monitors.size();
        long upCount     = monitors.stream().filter(m -> "UP".equals(m.getCurrentStatus())).count();
        long downCount   = monitors.stream().filter(m -> "DOWN".equals(m.getCurrentStatus())).count();
        long maintCount  = monitors.stream().filter(m -> "MAINTENANCE".equals(m.getCurrentStatus())).count();
        long unknownCount = total - upCount - downCount - maintCount;

        long openIncidents = incidentRepository.countByWorkspaceIdAndStatus(workspaceId, "OPEN");

        if (monitors.isEmpty()) {
            DashboardSummaryResponse summary = new DashboardSummaryResponse();
            summary.setTotalMonitors(0);
            return summary;
        }

        List<UUID> monitorIds = monitors.stream().map(Monitor::getId).collect(Collectors.toList());
        List<MonitorStats> allStats = monitorStatsRepository.findAllById(monitorIds);

        // Average uptime across all monitors from precomputed stats
        double avgUptime = allStats.stream()
                .filter(s -> s.getUptime() != null)
                .mapToDouble(s -> s.getUptime())
                .average()
                .orElse(0.0);

        // Rolling 24h average response time across all monitors
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        Double bulkAvg = monitorResultRepository.avgResponseTimeSinceIn(monitorIds, since24h);
        double total24hAvg = bulkAvg != null ? bulkAvg : 0.0;

        DashboardSummaryResponse summary = new DashboardSummaryResponse();
        summary.setTotalMonitors(total);
        summary.setUpCount(upCount);
        summary.setDownCount(downCount);
        summary.setUnknownCount(unknownCount);
        summary.setMaintenanceCount(maintCount);
        summary.setOpenIncidents(openIncidents);
        summary.setAvgUptimePercent(round2(avgUptime));
        summary.setAvgResponseTimeLast24Hours(Math.round(total24hAvg));
        return summary;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
