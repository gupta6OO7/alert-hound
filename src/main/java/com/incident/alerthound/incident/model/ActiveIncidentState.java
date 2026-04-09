package com.incident.alerthound.incident.model;

import java.time.Instant;
import java.util.UUID;

public record ActiveIncidentState(
        UUID incidentId,
        String service,
        IncidentStatus status,
        String severity,
        double errorRate,
        Instant startTime,
        Instant lastUpdated
) {
}
