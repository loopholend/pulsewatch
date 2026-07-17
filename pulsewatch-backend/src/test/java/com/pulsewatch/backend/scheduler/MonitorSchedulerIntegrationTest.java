package com.pulsewatch.backend.scheduler;

import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MonitorSchedulerIntegrationTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitorExecutionService monitorExecutionService;

    @Mock
    private ScheduledExecutorService schedulerPool;

    @InjectMocks
    private MonitorScheduler monitorScheduler;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(monitorScheduler, "schedulerPool", schedulerPool);
        ReflectionTestUtils.setField(monitorScheduler, "threadPoolSize", 20);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testScheduleMonitor() {
        Monitor monitor = new Monitor();
        monitor.setId(UUID.randomUUID());
        monitor.setIntervalSeconds(60);

        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        when(schedulerPool.scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn((ScheduledFuture) mockFuture);

        monitorScheduler.scheduleMonitor(monitor);

        verify(schedulerPool).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testUnscheduleMonitor() {
        UUID monitorId = UUID.randomUUID();
        monitorScheduler.unscheduleMonitor(monitorId);
        // Verify no interactions with schedulerPool since task wasn't in active tasks map
        verify(schedulerPool, never()).shutdown();
    }
}
