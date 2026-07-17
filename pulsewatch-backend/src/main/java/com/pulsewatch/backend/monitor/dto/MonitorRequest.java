package com.pulsewatch.backend.monitor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public class MonitorRequest {

    @NotBlank
    private String name;

    @NotBlank
    @org.hibernate.validator.constraints.URL(message = "Must be a valid URL")
    private String url;

    @NotNull
    @Pattern(regexp = "HTTP|TCP|PING|DATABASE", message = "Monitor type must be HTTP, TCP, PING, or DATABASE")
    private String monitorType;

    @NotBlank
    @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS", message = "Must be a valid HTTP method")
    private String method;

    private Map<String, String> headersJson;

    @Min(30)
    private int intervalSeconds = 300;

    @Min(1000)
    private int timeoutMs = 5000;

    private int expectedStatus = 200;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMonitorType() { return monitorType; }
    public void setMonitorType(String monitorType) { this.monitorType = monitorType; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Map<String, String> getHeadersJson() { return headersJson; }
    public void setHeadersJson(Map<String, String> headersJson) { this.headersJson = headersJson; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(int expectedStatus) { this.expectedStatus = expectedStatus; }

    @jakarta.validation.constraints.AssertTrue(message = "Timeout cannot exceed the interval")
    public boolean isTimeoutValid() {
        return timeoutMs <= (intervalSeconds * 1000);
    }
}
