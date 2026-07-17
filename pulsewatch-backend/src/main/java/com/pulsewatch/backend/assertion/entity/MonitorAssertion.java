package com.pulsewatch.backend.assertion.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An assertion applied after each monitor check.
 * All assertions must pass for a check to be marked successful.
 *
 * assertType values: STATUS_CODE, BODY_CONTAINS, BODY_NOT_CONTAINS
 * operator values:   EQ, CONTAINS, NOT_CONTAINS
 */
@Entity
@Table(name = "monitor_assertions")
public class MonitorAssertion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "monitor_id", nullable = false)
    private UUID monitorId;

    /**
     * STATUS_CODE: compare HTTP status code
     * BODY_CONTAINS: check that response body contains expected string
     * BODY_NOT_CONTAINS: check that response body does NOT contain expected string
     */
    @Column(name = "assert_type", nullable = false, length = 50)
    private String assertType;

    /**
     * EQ (equals), CONTAINS, NOT_CONTAINS
     */
    @Column(nullable = false, length = 20)
    private String operator;

    /**
     * For STATUS_CODE: "200", "201", etc.
     * For BODY_*: the string to search for.
     */
    @Column(nullable = false, length = 500)
    private String expected;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public UUID getMonitorId() { return monitorId; }
    public void setMonitorId(UUID monitorId) { this.monitorId = monitorId; }
    public String getAssertType() { return assertType; }
    public void setAssertType(String assertType) { this.assertType = assertType; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
