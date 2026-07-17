package com.pulsewatch.backend.scheduler;

import com.pulsewatch.backend.monitor.repository.MonitorResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Periodically prunes historical monitor check results to prevent unbounded database growth.
 * Default retention is 30 days. Runs daily at 2:00 AM.
 */
@Component
public class DataPruningScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataPruningScheduler.class);

    @Autowired
    private MonitorResultRepository monitorResultRepository;

    @Value("${pulsewatch.data.pruning.days-to-keep:30}")
    private int daysToKeep;

    @Value("${pulsewatch.data.pruning.enabled:true}")
    private boolean pruningEnabled;

    @Scheduled(cron = "${pulsewatch.data.pruning.cron:0 0 2 * * ?}")
    public void pruneOldData() {
        if (!pruningEnabled) {
            logger.info("Data pruning is disabled by configuration");
            return;
        }

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
            logger.info("Starting historical data pruning. Deleting monitor results older than {}", cutoff);
            int deletedCount = monitorResultRepository.deleteByCheckedAtBefore(cutoff);
            logger.info("Pruning completed. Deleted {} historical check result records", deletedCount);
        } catch (Exception e) {
            logger.error("Error occurred during monitor results data pruning: {}", e.getMessage(), e);
        }
    }
}
