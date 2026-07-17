package com.pulsewatch.backend.export.controller;

import com.pulsewatch.backend.auth.security.services.UserDetailsImpl;
import com.pulsewatch.backend.incident.entity.Incident;
import com.pulsewatch.backend.incident.service.IncidentService;
import com.pulsewatch.backend.monitor.entity.MonitorResult;
import com.pulsewatch.backend.monitor.service.MonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration (MockMvc) tests for ExportController.
 * Verifies that CSV/JSON export endpoints produce correct headers and content.
 *
 * UserDetailsImpl now requires (id, email, password, workspaceId, workspaceRole, authorities).
 * The workspace-scoped mock user is wired via a custom HandlerMethodArgumentResolver.
 */
@ExtendWith(MockitoExtension.class)
public class ExportControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock private IncidentService incidentService;
    @Mock private MonitorService monitorService;

    @InjectMocks
    private ExportController exportController;

    // Correct constructor: (UUID id, String email, String password, UUID workspaceId, String workspaceRole, Collection<? extends GrantedAuthority> authorities)
    private final UserDetailsImpl mockUserDetails = new UserDetailsImpl(
            UUID.randomUUID(),
            "testuser@example.com",
            "pass",
            UUID.randomUUID(), // workspaceId
            "OWNER",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(exportController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(UserDetailsImpl.class);
                    }
                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockUserDetails;
                    }
                })
                .build();
    }

    @Test
    void testExportIncidentsCsv() throws Exception {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setMonitorId(UUID.randomUUID());
        incident.setStatus("OPEN");
        incident.setSeverity("HIGH");
        incident.setStartedAt(LocalDateTime.now());

        // getAllIncidents(workspaceId) — workspace-scoped
        when(incidentService.getAllIncidents(any(UUID.class))).thenReturn(Collections.singletonList(incident));

        mockMvc.perform(get("/api/export/incidents").param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"incidents.csv\""))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id,monitorId,status,severity,startedAt,resolvedAt,durationSeconds")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(incident.getId().toString())));
    }

    @Test
    void testExportMonitorHistoryCsv() throws Exception {
        UUID monitorId = UUID.randomUUID();
        MonitorResult result = new MonitorResult();
        result.setMonitorId(monitorId);
        result.setStatusCode(200);
        result.setResponseTimeMs(150);
        result.setSuccess(true);
        ReflectionTestUtils.setField(result, "id", UUID.randomUUID());

        // getMonitorHistory(monitorId, window, workspaceId) — workspace-scoped
        when(monitorService.getMonitorHistory(any(UUID.class), anyString(), any(UUID.class)))
                .thenReturn(Collections.singletonList(result));

        mockMvc.perform(get("/api/export/monitor-history/" + monitorId).param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"monitor-history-" + monitorId + ".csv\""))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id,monitorId,checkedAt,statusCode,responseTimeMs,success")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("200")));
    }
}
