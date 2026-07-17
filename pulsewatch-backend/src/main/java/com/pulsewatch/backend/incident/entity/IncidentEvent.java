package com.pulsewatch.backend.incident.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "incident_events")
public class IncidentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // INCIDENT_OPENED, ALERT_SENT, SEVERITY_ESCALATED, INCIDENT_RESOLVED

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public IncidentEvent() {}

    public IncidentEvent(UUID incidentId, String eventType, String message) {
        this.incidentId = incidentId;
        this.eventType = eventType;
        this.message = message;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
