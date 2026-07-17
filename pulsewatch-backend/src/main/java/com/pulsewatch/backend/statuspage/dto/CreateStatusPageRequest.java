package com.pulsewatch.backend.statuspage.dto;

import java.util.List;
import java.util.UUID;

public class CreateStatusPageRequest {
    private String slug;
    private String title;
    private String description;
    private List<UUID> monitorIds; // monitors to add as services

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<UUID> getMonitorIds() { return monitorIds; }
    public void setMonitorIds(List<UUID> monitorIds) { this.monitorIds = monitorIds; }
}
