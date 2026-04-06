package com.incident.alerthound.detection.service;

public record DetectionSnapshot(
        long totalLogs,
        long errorLogs
) {

    public double errorRate() {
        if (totalLogs == 0) {
            return 0.0d;
        }
        return (double) errorLogs / totalLogs;
    }
}
