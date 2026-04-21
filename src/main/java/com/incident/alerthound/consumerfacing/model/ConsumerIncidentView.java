package com.incident.alerthound.consumerfacing.model;

import com.incident.alerthound.incident.model.IncidentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConsumerIncidentView(
        UUID incidentId,
        String service,
        IncidentStatus status,
        String severity,
        double errorRate,
        String summary,
        String rootCause,
        List<String> recommendations,
        Instant updatedAt,
        Instant lastDetectedAt,
        Instant viewUpdatedAt
) {
}
