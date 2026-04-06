package com.incident.alerthound.detection.api;

import com.incident.alerthound.detection.model.IncidentCreatedEvent;
import java.time.Instant;

public record IncidentDebugResponse(
        String service,
        Instant windowStart,
        Instant windowEnd,
        long totalLogs,
        long errorLogs,
        double errorRate,
        long minimumTotalLogs,
        double errorRateThreshold,
        boolean wouldTriggerIncident,
        String evaluationReason,
        boolean activeIncident,
        Instant activeIncidentExpiresAt,
        IncidentCreatedEvent lastIncident
) {
}
