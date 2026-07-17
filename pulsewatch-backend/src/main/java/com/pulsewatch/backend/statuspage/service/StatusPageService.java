package com.pulsewatch.backend.statuspage.service;

import com.pulsewatch.backend.exception.ResourceNotFoundException;
import com.pulsewatch.backend.incident.repository.IncidentRepository;
import com.pulsewatch.backend.analytics.entity.MonitorStats;
import com.pulsewatch.backend.analytics.repository.MonitorStatsRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import com.pulsewatch.backend.statuspage.dto.CreateStatusPageRequest;
import com.pulsewatch.backend.statuspage.dto.PublicStatusPageResponse;
import com.pulsewatch.backend.statuspage.entity.ServiceStatus;
import com.pulsewatch.backend.statuspage.entity.StatusPage;
import com.pulsewatch.backend.statuspage.repository.ServiceStatusRepository;
import com.pulsewatch.backend.statuspage.repository.StatusPageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StatusPageService {

    private static final Logger logger = LoggerFactory.getLogger(StatusPageService.class);

    @Autowired private StatusPageRepository statusPageRepository;
    @Autowired private ServiceStatusRepository serviceStatusRepository;
    @Autowired private MonitorRepository monitorRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private MonitorStatsRepository monitorStatsRepository;

    // ── CRUD ────────────────────────────────────────────────────────────────

    @Transactional
    public StatusPage create(CreateStatusPageRequest request, UUID workspaceId) {
        if (statusPageRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug '" + request.getSlug() + "' is already taken.");
        }

        StatusPage page = new StatusPage();
        page.setWorkspaceId(workspaceId);
        page.setSlug(request.getSlug().toLowerCase().trim());
        page.setTitle(request.getTitle());
        page.setDescription(request.getDescription());
        StatusPage saved = statusPageRepository.save(page);

        // Add monitors as services
        if (request.getMonitorIds() != null) {
            for (UUID monitorId : request.getMonitorIds()) {
                addService(saved.getId(), monitorId, workspaceId);
            }
        }

        return saved;
    }

    @Transactional
    public StatusPage update(UUID pageId, CreateStatusPageRequest request, UUID workspaceId) {
        StatusPage page = getOwnedPage(pageId, workspaceId);

        // If slug is changing, verify it's not taken
        if (!page.getSlug().equals(request.getSlug()) && statusPageRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug '" + request.getSlug() + "' is already taken.");
        }

        page.setSlug(request.getSlug().toLowerCase().trim());
        page.setTitle(request.getTitle());
        page.setDescription(request.getDescription());
        return statusPageRepository.save(page);
    }

    @Transactional
    public void delete(UUID pageId, UUID workspaceId) {
        StatusPage page = getOwnedPage(pageId, workspaceId);
        statusPageRepository.delete(page);
    }

    public List<StatusPage> listByUser(UUID workspaceId) {
        return statusPageRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    // ── Service management ───────────────────────────────────────────────────

    @Transactional
    public ServiceStatus addService(UUID pageId, UUID monitorId, UUID workspaceId) {
        getOwnedPage(pageId, workspaceId); // verify ownership

        Monitor monitor = monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + monitorId));

        // Idempotent: if already exists, return existing
        return serviceStatusRepository.findByStatusPageIdAndMonitorId(pageId, monitorId)
                .orElseGet(() -> {
                    ServiceStatus ss = new ServiceStatus();
                    ss.setStatusPageId(pageId);
                    ss.setMonitorId(monitorId);
                    ss.setServiceName(monitor.getName());
                    ss.setCurrentStatus(monitor.getCurrentStatus());
                    ss.setLastChecked(LocalDateTime.now());
                    return serviceStatusRepository.save(ss);
                });
    }

    @Transactional
    public void removeService(UUID pageId, UUID monitorId, UUID workspaceId) {
        getOwnedPage(pageId, workspaceId);
        serviceStatusRepository.findByStatusPageIdAndMonitorId(pageId, monitorId)
                .ifPresent(serviceStatusRepository::delete);
    }

    // ── Public endpoint ──────────────────────────────────────────────────────

    /**
     * Public — no authentication required.
     * Reads only from service_status, never from monitor_results.
     */
    public PublicStatusPageResponse getPublicPage(String slug) {
        StatusPage page = statusPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found: " + slug));

        List<ServiceStatus> services = serviceStatusRepository.findByStatusPageIdOrderByServiceNameAsc(page.getId());

        // Compute overall status
        String overallStatus = computeOverallStatus(services);

        // Check for active incident across all monitors on this page
        boolean hasActiveIncident = services.stream().anyMatch(ss ->
                incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(ss.getMonitorId(), "OPEN").isPresent()
        );

        // Find most recent lastChecked
        LocalDateTime lastUpdated = services.stream()
                .map(ServiceStatus::getLastChecked)
                .filter(t -> t != null)
                .max(LocalDateTime::compareTo)
                .orElse(page.getUpdatedAt());

        // Build response
        PublicStatusPageResponse response = new PublicStatusPageResponse();
        response.setId(page.getId());
        response.setSlug(page.getSlug());
        response.setTitle(page.getTitle());
        response.setDescription(page.getDescription());
        response.setOverallStatus(overallStatus);
        response.setLastUpdated(lastUpdated);
        response.setHasActiveIncident(hasActiveIncident);

        List<PublicStatusPageResponse.ServiceStatusItem> items = services.stream()
                .map(ss -> {
                    PublicStatusPageResponse.ServiceStatusItem item = new PublicStatusPageResponse.ServiceStatusItem();
                    item.setMonitorId(ss.getMonitorId());
                    item.setServiceName(ss.getServiceName() != null ? ss.getServiceName() : "Service");
                    item.setCurrentStatus(ss.getCurrentStatus());
                    item.setLastChecked(ss.getLastChecked());
                    // Enrich with pre-computed stats (no extra queries per service beyond a single findById)
                    monitorStatsRepository.findById(ss.getMonitorId()).ifPresent(stats -> {
                        item.setUptimePercent(stats.getUptime());
                        item.setAvgResponseTimeMs(stats.getAvgLatency());
                    });
                    return item;
                })
                .collect(Collectors.toList());

        response.setServices(items);
        return response;
    }

    /**
     * Called by the scheduler after each monitor check to keep service_status fresh.
     */
    @Transactional
    public void syncMonitorStatus(UUID monitorId, String newStatus) {
        List<ServiceStatus> entries = serviceStatusRepository.findByMonitorId(monitorId);
        for (ServiceStatus ss : entries) {
            ss.setCurrentStatus(newStatus);
            ss.setLastChecked(LocalDateTime.now());
            serviceStatusRepository.save(ss);
        }
        if (!entries.isEmpty()) {
            logger.debug("Synced status {} for monitor {} to {} service_status entries", newStatus, monitorId, entries.size());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private StatusPage getOwnedPage(UUID pageId, UUID workspaceId) {
        return statusPageRepository.findByIdAndWorkspaceId(pageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found in active workspace"));
    }

    private String computeOverallStatus(List<ServiceStatus> services) {
        if (services.isEmpty()) {
            return "UNKNOWN";
        }
        long downCount = services.stream().filter(s -> "DOWN".equals(s.getCurrentStatus())).count();
        long totalCount = services.size();
        if (downCount == 0) {
            return "UP";
        }
        if (downCount == totalCount) {
            return "DOWN";
        }
        return "DEGRADED";
    }
}
