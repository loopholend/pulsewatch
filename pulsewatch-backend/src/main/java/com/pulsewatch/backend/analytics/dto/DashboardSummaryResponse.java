package com.pulsewatch.backend.analytics.dto;

public class DashboardSummaryResponse {
    private long totalMonitors;
    private long upCount;
    private long downCount;
    private long unknownCount;
    private long maintenanceCount;
    private long openIncidents;
    private double avgUptimePercent;
    private long avgResponseTimeLast24Hours;

    public long getTotalMonitors() { return totalMonitors; }
    public void setTotalMonitors(long totalMonitors) { this.totalMonitors = totalMonitors; }
    public long getUpCount() { return upCount; }
    public void setUpCount(long upCount) { this.upCount = upCount; }
    public long getDownCount() { return downCount; }
    public void setDownCount(long downCount) { this.downCount = downCount; }
    public long getUnknownCount() { return unknownCount; }
    public void setUnknownCount(long unknownCount) { this.unknownCount = unknownCount; }
    public long getMaintenanceCount() { return maintenanceCount; }
    public void setMaintenanceCount(long maintenanceCount) { this.maintenanceCount = maintenanceCount; }
    public long getOpenIncidents() { return openIncidents; }
    public void setOpenIncidents(long openIncidents) { this.openIncidents = openIncidents; }
    public double getAvgUptimePercent() { return avgUptimePercent; }
    public void setAvgUptimePercent(double avgUptimePercent) { this.avgUptimePercent = avgUptimePercent; }
    public long getAvgResponseTimeLast24Hours() { return avgResponseTimeLast24Hours; }
    public void setAvgResponseTimeLast24Hours(long avgResponseTimeLast24Hours) { this.avgResponseTimeLast24Hours = avgResponseTimeLast24Hours; }
}
