package com.incident.alerthound.detection.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record IncidentCreatedEvent(
        String incidentId,
        String service,
        Instant windowStart,
        Instant windowEnd,
        Instant triggeredAt,
        long totalLogs,
        long errorLogs,
        double errorRate,
        String severity,
        String reason
) {
}
