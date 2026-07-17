package com.pulsewatch.backend.incident.service;

import com.pulsewatch.backend.incident.entity.Incident;
import com.pulsewatch.backend.incident.entity.IncidentEvent;
import com.pulsewatch.backend.incident.event.IncidentOpenedEvent;
import com.pulsewatch.backend.incident.event.IncidentResolvedEvent;
import com.pulsewatch.backend.incident.repository.IncidentEventRepository;
import com.pulsewatch.backend.incident.repository.IncidentRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentEventRepository incidentEventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MonitorRepository monitorRepository;

    @InjectMocks
    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(incidentService, "failureThreshold", 3);
    }

    @Test
    void testHandleFailure_belowThreshold() {
        UUID monitorId = UUID.randomUUID();
        incidentService.handleFailure(monitorId, 2, null);

        verify(incidentRepository, never()).findFirstByMonitorIdAndStatusOrderByStartedAtDesc(any(), any());
        verify(incidentRepository, never()).findFirstByMonitorIdAndStatusInOrderByStartedAtDesc(any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void testHandleFailure_createsNewIncident() {
        UUID monitorId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        
        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        monitor.setWorkspaceId(workspaceId);
        
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(incidentRepository.findFirstByMonitorIdAndStatusInOrderByStartedAtDesc(eq(monitorId), anyList()))
                .thenReturn(Optional.empty());

        incidentService.handleFailure(monitorId, 3, "Read timeout");

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());

        Incident savedIncident = incidentCaptor.getValue();
        assertEquals(monitorId, savedIncident.getMonitorId());
        assertEquals(workspaceId, savedIncident.getWorkspaceId());
        assertEquals("OPEN", savedIncident.getStatus());
        assertEquals("MEDIUM", savedIncident.getSeverity()); // failures = 3
        assertEquals("Read timeout", savedIncident.getFailureReason());

        verify(incidentEventRepository).save(any(IncidentEvent.class));
        verify(eventPublisher).publishEvent(any(IncidentOpenedEvent.class));
    }

    @Test
    void testHandleFailure_escalatesSeverity() {
        UUID monitorId = UUID.randomUUID();
        Incident existingIncident = new Incident();
        existingIncident.setId(UUID.randomUUID());
        existingIncident.setMonitorId(monitorId);
        existingIncident.setStatus("OPEN");
        existingIncident.setSeverity("MEDIUM");

        when(incidentRepository.findFirstByMonitorIdAndStatusInOrderByStartedAtDesc(eq(monitorId), anyList()))
                .thenReturn(Optional.of(existingIncident));

        // 5 failures -> HIGH severity
        incidentService.handleFailure(monitorId, 5, "Connection Refused");

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());

        Incident savedIncident = incidentCaptor.getValue();
        assertEquals("HIGH", savedIncident.getSeverity());

        verify(incidentEventRepository).save(any(IncidentEvent.class));
        // eventPublisher shouldn't be called on escalation per current code
        verify(eventPublisher, never()).publishEvent(any(IncidentOpenedEvent.class));
    }

    @Test
    void testHandleSuccess_resolvesIncident() {
        UUID monitorId = UUID.randomUUID();
        Incident existingIncident = new Incident();
        existingIncident.setId(UUID.randomUUID());
        existingIncident.setMonitorId(monitorId);
        existingIncident.setStatus("OPEN");
        existingIncident.setStartedAt(LocalDateTime.now().minusMinutes(5));

        when(incidentRepository.findFirstByMonitorIdAndStatusInOrderByStartedAtDesc(eq(monitorId), anyList()))
                .thenReturn(Optional.of(existingIncident));

        incidentService.handleSuccess(monitorId);

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());

        Incident savedIncident = incidentCaptor.getValue();
        assertEquals("RESOLVED", savedIncident.getStatus());
        assertNotNull(savedIncident.getResolvedAt());
        assertTrue(savedIncident.getDurationSeconds() >= 300); // approx 5 mins

        verify(incidentEventRepository).save(any(IncidentEvent.class));
        verify(eventPublisher).publishEvent(any(IncidentResolvedEvent.class));
    }

    @Test
    void testHandleSuccess_noOpenIncident() {
        UUID monitorId = UUID.randomUUID();
        when(incidentRepository.findFirstByMonitorIdAndStatusInOrderByStartedAtDesc(eq(monitorId), anyList()))
                .thenReturn(Optional.empty());

        incidentService.handleSuccess(monitorId);

        verify(incidentRepository, never()).save(any());
        verify(incidentEventRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(IncidentResolvedEvent.class));
    }
}
