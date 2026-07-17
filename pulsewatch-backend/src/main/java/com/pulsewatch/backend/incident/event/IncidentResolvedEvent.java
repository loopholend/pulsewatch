package com.pulsewatch.backend.incident.event;

import com.pulsewatch.backend.incident.entity.Incident;
import org.springframework.context.ApplicationEvent;

public class IncidentResolvedEvent extends ApplicationEvent {
    private final Incident incident;

    public IncidentResolvedEvent(Object source, Incident incident) {
        super(source);
        this.incident = incident;
    }

    public Incident getIncident() {
        return incident;
    }
}
