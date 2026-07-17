package com.pulsewatch.backend.exception;

import com.pulsewatch.backend.monitor.entity.Monitor;

public class DuplicateMonitorException extends RuntimeException {
    private final Monitor existingMonitor;

    public DuplicateMonitorException(Monitor existingMonitor) {
        super("Monitor already exists");
        this.existingMonitor = existingMonitor;
    }

    public Monitor getExistingMonitor() {
        return existingMonitor;
    }
}
