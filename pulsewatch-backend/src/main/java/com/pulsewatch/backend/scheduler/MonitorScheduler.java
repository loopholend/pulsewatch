package com.pulsewatch.backend.scheduler;

import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Per-monitor scheduler: each active monitor fires independently at its own
 * configured {@code intervalSeconds}, rather than all monitors sharing a
 * single global tick.
 *
 * <p>On startup, all currently-active monitors are scheduled with a small
 * staggered initial delay to avoid a thundering-herd effect on startup. Any
 * subsequent create / update / delete / toggle operation MUST call
 * {@link #scheduleMonitor(Monitor)} or {@link #unscheduleMonitor(UUID)} so
 * the task map stays consistent.</p>
 *
 * <p>Uses {@code scheduleWithFixedDelay} so the next execution begins only
 * after the previous one has fully completed, preventing overlapping checks
 * if a target is slow to respond.</p>
 */
@Component
public class MonitorScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MonitorScheduler.class);

    /** Active scheduled tasks, keyed by monitor ID. Thread-safe. */
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private ScheduledExecutorService schedulerPool;

    @Value("${pulsewatch.scheduler.thread-pool-size:20}")
    private int threadPoolSize;

    @Autowired private MonitorRepository monitorRepository;
    @Autowired private MonitorExecutionService monitorExecutionService;

    // -- Lifecycle -----------------------------------------------------------

    @PostConstruct
    public void init() {
        schedulerPool = Executors.newScheduledThreadPool(threadPoolSize);
        List<Monitor> activeMonitors = monitorRepository.findByActiveTrue();
        logger.info("Bootstrapping per-monitor scheduler for {} active monitors", activeMonitors.size());

        // Stagger initial delays (2 s apart) to avoid a thundering herd on startup.
        for (int i = 0; i < activeMonitors.size(); i++) {
            scheduleMonitor(activeMonitors.get(i), (long) i * 2);
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down MonitorScheduler -- cancelling {} tasks", scheduledTasks.size());
        scheduledTasks.values().forEach(f -> f.cancel(false));
        scheduledTasks.clear();
        if (schedulerPool != null) {
            schedulerPool.shutdown();
            try {
                if (!schedulerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    schedulerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                schedulerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("MonitorScheduler shut down successfully");
    }

    // -- Public API (called by MonitorService on CRUD / toggle) --------------

    /**
     * Schedule (or re-schedule) a monitor using its current {@code intervalSeconds}.
     * Cancels any existing task for this monitor ID before creating a new one.
     * Safe to call multiple times for the same monitor (idempotent cancel + schedule).
     *
     * @param monitor the monitor to schedule; must be active and non-null
     */
    public void scheduleMonitor(Monitor monitor) {
        scheduleMonitor(monitor, 0L);
    }

    /**
     * Remove and cancel the scheduled task for the given monitor ID.
     * No-op if the monitor is not currently scheduled.
     *
     * @param monitorId the ID of the monitor to unschedule
     */
    public void unscheduleMonitor(UUID monitorId) {
        ScheduledFuture<?> existing = scheduledTasks.remove(monitorId);
        if (existing != null) {
            existing.cancel(false);
            logger.debug("Unscheduled monitor {}", monitorId);
        }
    }

    // -- Private helpers -----------------------------------------------------

    private void scheduleMonitor(Monitor monitor, long initialDelaySecs) {
        UUID monitorId = monitor.getId();
        long intervalSecs = monitor.getIntervalSeconds();

        // Cancel any existing task first (handles re-scheduling after update)
        unscheduleMonitor(monitorId);

        ScheduledFuture<?> future = schedulerPool.scheduleWithFixedDelay(
                () -> runCheck(monitorId),
                initialDelaySecs,
                intervalSecs,
                TimeUnit.SECONDS
        );

        scheduledTasks.put(monitorId, future);
        logger.info("Scheduled monitor {} (interval={}s, initialDelay={}s)",
                monitorId, intervalSecs, initialDelaySecs);
    }

    /**
     * Executed by the scheduler pool for each due monitor check.
     * Re-fetches the monitor from the database to ensure up-to-date configuration
     * (URL, timeout, headers, etc.) is always used -- especially important after edits.
     * Handles its own exceptions to prevent the scheduled task from dying silently.
     */
    private void runCheck(UUID monitorId) {
        try {
            Monitor monitor = monitorRepository.findById(monitorId).orElse(null);
            if (monitor == null || !monitor.isActive()) {
                // Monitor was soft-deleted or deactivated between checks; clean up.
                unscheduleMonitor(monitorId);
                return;
            }
            monitorExecutionService.execute(monitor);
        } catch (Exception e) {
            logger.error("Uncaught error during check for monitor {}: {}", monitorId, e.getMessage(), e);
        }
    }
}
