package com.incident.alerthound.incident.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record IncidentUpdatedEvent(
        UUID incidentId,
        String service,
        IncidentStatus status,
        String severity,
        double errorRate,
        String summary,
        String rootCause,
        List<String> recommendations,
        Instant updatedAt,
        Instant lastDetectedAt
) {
}
