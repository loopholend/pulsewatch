package com.pulsewatch.backend.analytics.dto;

import java.time.LocalDateTime;

public class LatencyDataPoint {
    private LocalDateTime checkedAt;
    private Integer responseTimeMs;
    private boolean success;

    public LatencyDataPoint(LocalDateTime checkedAt, Integer responseTimeMs, boolean success) {
        this.checkedAt = checkedAt;
        this.responseTimeMs = responseTimeMs;
        this.success = success;
    }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public boolean isSuccess() { return success; }
}
