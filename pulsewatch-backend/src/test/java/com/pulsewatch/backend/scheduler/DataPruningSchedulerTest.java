package com.pulsewatch.backend.scheduler;

import com.pulsewatch.backend.monitor.repository.MonitorResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataPruningSchedulerTest {

    @Mock
    private MonitorResultRepository monitorResultRepository;

    @InjectMocks
    private DataPruningScheduler pruningScheduler;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(pruningScheduler, "daysToKeep", 30);
        ReflectionTestUtils.setField(pruningScheduler, "pruningEnabled", true);
    }

    @Test
    void testPruneOldData_enabled() {
        when(monitorResultRepository.deleteByCheckedAtBefore(any(LocalDateTime.class))).thenReturn(150);

        pruningScheduler.pruneOldData();

        verify(monitorResultRepository).deleteByCheckedAtBefore(any(LocalDateTime.class));
    }

    @Test
    void testPruneOldData_disabled() {
        ReflectionTestUtils.setField(pruningScheduler, "pruningEnabled", false);

        pruningScheduler.pruneOldData();

        verify(monitorResultRepository, never()).deleteByCheckedAtBefore(any(LocalDateTime.class));
    }
}
