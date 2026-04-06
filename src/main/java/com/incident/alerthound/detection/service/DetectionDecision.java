package com.incident.alerthound.detection.service;

import java.time.Instant;

public record DetectionDecision(
        boolean incidentTriggered,
        long totalLogs,
        long errorLogs,
        double errorRate,
        Instant windowStart,
        Instant windowEnd,
        String reason
) {

    public static DetectionDecision noIncident(
            long totalLogs,
            long errorLogs,
            double errorRate,
            Instant windowStart,
            Instant windowEnd,
            String reason
    ) {
        return new DetectionDecision(false, totalLogs, errorLogs, errorRate, windowStart, windowEnd, reason);
    }

    public static DetectionDecision incident(
            long totalLogs,
            long errorLogs,
            double errorRate,
            Instant windowStart,
            Instant windowEnd,
            String reason
    ) {
        return new DetectionDecision(true, totalLogs, errorLogs, errorRate, windowStart, windowEnd, reason);
    }
}
