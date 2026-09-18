package com.pulsewatch.backend.alert.service;

import com.pulsewatch.backend.alert.dto.AlertRuleRequest;
import com.pulsewatch.backend.alert.entity.AlertHistory;
import com.pulsewatch.backend.alert.entity.AlertRule;
import com.pulsewatch.backend.alert.provider.AlertContext;
import com.pulsewatch.backend.alert.provider.AlertProvider;
import com.pulsewatch.backend.alert.repository.AlertHistoryRepository;
import com.pulsewatch.backend.alert.repository.AlertRuleRepository;
import com.pulsewatch.backend.exception.ResourceNotFoundException;
import com.pulsewatch.backend.incident.event.IncidentOpenedEvent;
import com.pulsewatch.backend.incident.event.IncidentResolvedEvent;
import com.pulsewatch.backend.incident.entity.Incident;
import com.pulsewatch.backend.incident.entity.IncidentEvent;
import com.pulsewatch.backend.incident.repository.IncidentEventRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;
    @Mock
    private AlertHistoryRepository alertHistoryRepository;
    @Mock
    private AlertProvider alertProvider;
    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private IncidentEventRepository incidentEventRepository;

    @InjectMocks
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        lenient().when(alertProvider.supports(any(AlertContext.class))).thenReturn(true);
        ReflectionTestUtils.setField(alertService, "alertProviders", Arrays.asList(alertProvider));
    }

    @Test
    void testProcessIncidentOpened() throws Exception {
        UUID monitorId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setMonitorId(monitorId);
        incident.setSeverity("HIGH");

        Monitor monitor = new Monitor();
        monitor.setName("Test Monitor");
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        AlertRule rule = new AlertRule();
        ReflectionTestUtils.setField(rule, "id", UUID.randomUUID());
        rule.setNotificationEmail("test@example.com");
        rule.setCooldownMinutes(30);
        // cooldown expired by default because lastAlertSentAt is null

        when(alertRuleRepository.findByMonitorIdAndRuleTypeAndEnabledTrue(monitorId, "INCIDENT_OPENED"))
                .thenReturn(Collections.singletonList(rule));

        alertService.processIncidentOpened(new IncidentOpenedEvent(this, incident));

        verify(alertProvider).sendAlert(any(AlertContext.class));
        verify(alertHistoryRepository).save(any(AlertHistory.class));
        verify(incidentEventRepository).save(any(IncidentEvent.class));
        verify(alertRuleRepository).save(rule);
        assertNotNull(rule.getLastAlertSentAt());
    }

    @Test
    void testProcessIncidentOpened_cooldownActive() throws Exception {
        UUID monitorId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setMonitorId(monitorId);
        incident.setSeverity("HIGH");

        Monitor monitor = new Monitor();
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        AlertRule rule = new AlertRule();
        ReflectionTestUtils.setField(rule, "id", UUID.randomUUID());
        rule.setNotificationEmail("test@example.com");
        rule.setCooldownMinutes(30);
        rule.setLastAlertSentAt(LocalDateTime.now().minusMinutes(5)); // cooldown active

        when(alertRuleRepository.findByMonitorIdAndRuleTypeAndEnabledTrue(monitorId, "INCIDENT_OPENED"))
                .thenReturn(Collections.singletonList(rule));

        alertService.processIncidentOpened(new IncidentOpenedEvent(this, incident));

        verify(alertProvider, never()).sendAlert(any());
        verify(alertHistoryRepository, never()).save(any());
    }

    @Test
    void testProcessIncidentResolved() throws Exception {
        UUID monitorId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setMonitorId(monitorId);
        incident.setSeverity("HIGH");
        incident.setDurationSeconds(120L);

        Monitor monitor = new Monitor();
        monitor.setName("Test Monitor");
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        AlertRule rule = new AlertRule();
        ReflectionTestUtils.setField(rule, "id", UUID.randomUUID());
        rule.setNotificationEmail("test@example.com");

        when(alertRuleRepository.findByMonitorIdAndRuleTypeAndEnabledTrue(monitorId, "INCIDENT_RESOLVED"))
                .thenReturn(Collections.singletonList(rule));

        alertService.processIncidentResolved(new IncidentResolvedEvent(this, incident));

        verify(alertProvider).sendAlert(any(AlertContext.class));
        verify(alertHistoryRepository).save(any(AlertHistory.class));
        verify(incidentEventRepository).save(any(IncidentEvent.class));
    }

    @Test
    void testCreateRule() {
        UUID workspaceId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        
        AlertRuleRequest request = new AlertRuleRequest();
        request.setMonitorId(monitorId);
        request.setRuleType("INCIDENT_OPENED");
        request.setNotificationEmail("test@example.com");
        request.setCooldownMinutes(15);

        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        monitor.setWorkspaceId(workspaceId);

        when(monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)).thenReturn(Optional.of(monitor));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArguments()[0]);

        AlertRule savedRule = alertService.createRule(request, workspaceId);

        assertNotNull(savedRule);
        assertEquals("INCIDENT_OPENED", savedRule.getRuleType());
        assertEquals("test@example.com", savedRule.getNotificationEmail());
        assertEquals(15, savedRule.getCooldownMinutes());
        assertTrue(savedRule.getEnabled());

        // Regression guard: workspace_id must ALWAYS be set — core of this bug fix
        assertNotNull(savedRule.getWorkspaceId(),
            "workspaceId must never be null on a persisted AlertRule (workspace migration regression)");
        assertEquals(workspaceId, savedRule.getWorkspaceId(),
            "workspaceId must be derived from monitor.getWorkspaceId(), not from another source");

        // Verify the exact object passed to save() also had workspaceId set
        ArgumentCaptor<AlertRule> captor = ArgumentCaptor.forClass(AlertRule.class);
        verify(alertRuleRepository).save(captor.capture());
        assertNotNull(captor.getValue().getWorkspaceId(),
            "The AlertRule passed to repository.save() must have workspaceId populated");
    }

    @Test
    void testCreateRule_wrongUser() {
        UUID workspaceId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        
        AlertRuleRequest request = new AlertRuleRequest();
        request.setMonitorId(monitorId);

        when(monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            alertService.createRule(request, workspaceId);
        });
    }

    @Test
    void testToggleRule() {
        UUID ruleId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        AlertRule rule = new AlertRule();
        rule.setMonitorId(monitorId);
        ReflectionTestUtils.setField(rule, "id", ruleId);
        rule.setEnabled(true);

        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        monitor.setWorkspaceId(workspaceId);

        when(alertRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(monitorRepository.findByIdAndWorkspaceId(monitorId, workspaceId)).thenReturn(Optional.of(monitor));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(i -> i.getArguments()[0]);

        AlertRule toggled = alertService.toggleRule(ruleId, workspaceId);
        assertFalse(toggled.getEnabled());
    }
}
