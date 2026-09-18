package com.pulsewatch.backend.monitor;

import com.pulsewatch.backend.auth.entity.User;
import com.pulsewatch.backend.auth.entity.Workspace;
import com.pulsewatch.backend.auth.entity.WorkspaceMember;
import com.pulsewatch.backend.auth.repository.UserRepository;
import com.pulsewatch.backend.auth.repository.WorkspaceMemberRepository;
import com.pulsewatch.backend.auth.repository.WorkspaceRepository;
import com.pulsewatch.backend.monitor.entity.Monitor;
import com.pulsewatch.backend.monitor.repository.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MonitorRepository.
 * Verifies workspace-scoped monitor retrieval after the V11 workspace migration.
 * Previously this test used userId-scoped methods (findByUserId, countByUserId)
 * which no longer exist. Rewritten to use workspaceId-scoped equivalents.
 *
 * Requires a live PostgreSQL database. Run with: DB_INTEGRATION_TESTS=true ./mvnw test
 */
@SpringBootTest
@Transactional
@DisabledIfEnvironmentVariable(named = "DB_INTEGRATION_TESTS", matches = "(?!true).*",
    disabledReason = "Requires DB_INTEGRATION_TESTS=true env var and a live PostgreSQL database")
public class MonitorRepositoryTest {

    @Autowired private MonitorRepository monitorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;

    @Test
    void testSaveAndFindMonitorByWorkspace() {
        // Arrange: create user, workspace, and membership
        User user = new User("monitor_repo_test_" + UUID.randomUUID() + "@example.com", "pass", "USER");
        user = userRepository.saveAndFlush(user);

        // Use random slug to avoid unique constraint conflicts
        Workspace workspace = new Workspace("Test Workspace", "test-ws-" + UUID.randomUUID());
        workspace = workspaceRepository.saveAndFlush(workspace);

        WorkspaceMember member = new WorkspaceMember(workspace.getId(), user.getId(), "OWNER");
        workspaceMemberRepository.saveAndFlush(member);

        UUID workspaceId = workspace.getId();

        // Create monitor in this workspace
        Monitor monitor = new Monitor();
        monitor.setWorkspaceId(workspaceId);
        monitor.setCreatedBy(user.getId());
        monitor.setName("Test Monitor Integration");
        monitor.setUrl("https://example.com");
        monitor.setMonitorType("HTTP");
        monitor.setActive(true);
        monitor.setMethod("GET");
        monitor.setIntervalSeconds(60);
        monitor.setTimeoutMs(5000);
        monitor.setExpectedStatus(200);

        Monitor saved = monitorRepository.save(monitor);

        // Assert: basic save
        assertNotNull(saved.getId(), "Saved monitor should have an ID");
        assertEquals(workspaceId, saved.getWorkspaceId(), "Monitor should belong to the correct workspace");

        // Assert: findByWorkspaceId
        List<Monitor> workspaceMonitors = monitorRepository.findByWorkspaceId(workspaceId);
        assertEquals(1, workspaceMonitors.size(), "Workspace should have exactly 1 monitor");
        assertEquals("Test Monitor Integration", workspaceMonitors.get(0).getName());

        // Assert: findByIdAndWorkspaceId (workspace-scoped lookup)
        Optional<Monitor> found = monitorRepository.findByIdAndWorkspaceId(saved.getId(), workspaceId);
        assertTrue(found.isPresent(), "Should find monitor by id + workspaceId");

        // Assert: countByWorkspaceId
        long count = monitorRepository.countByWorkspaceId(workspaceId);
        assertEquals(1L, count, "countByWorkspaceId should return 1");

        // Assert: findByActiveTrue (monitor should appear in global active list)
        List<Monitor> activeMonitors = monitorRepository.findByActiveTrue();
        assertTrue(activeMonitors.stream().anyMatch(m -> m.getId().equals(saved.getId())),
                "Active monitor should appear in findByActiveTrue()");

        // Assert: cross-workspace isolation (different workspace gets no results)
        UUID otherWorkspaceId = UUID.randomUUID();
        List<Monitor> otherWorkspaceMonitors = monitorRepository.findByWorkspaceId(otherWorkspaceId);
        assertTrue(otherWorkspaceMonitors.stream().noneMatch(m -> m.getId().equals(saved.getId())),
                "Monitor should not appear in a different workspace's list");
    }
}
