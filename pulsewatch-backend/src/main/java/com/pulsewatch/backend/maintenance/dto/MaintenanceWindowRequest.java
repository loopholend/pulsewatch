package com.pulsewatch.backend.maintenance.dto;

import java.time.LocalDateTime;

public class MaintenanceWindowRequest {
    @jakarta.validation.constraints.NotNull(message = "Start time is required")
    private LocalDateTime startsAt;
    @jakarta.validation.constraints.NotNull(message = "End time is required")
    private LocalDateTime endsAt;
    @jakarta.validation.constraints.NotBlank(message = "Reason is required")
    private String reason;

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @jakarta.validation.constraints.AssertTrue(message = "End time must be after start time")
    public boolean isTimeValid() {
        if (startsAt == null || endsAt == null) return true; // Let @NotNull handle this
        return endsAt.isAfter(startsAt);
    }
}
