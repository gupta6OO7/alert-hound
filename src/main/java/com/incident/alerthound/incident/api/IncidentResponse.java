package com.incident.alerthound.incident.api;

import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.model.IncidentStatus;
import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String service,
        IncidentStatus status,
        String severity,
        double errorRate,
        Instant startTime,
        Instant endTime,
        String summary,
        String rootCause,
        Instant createdAt,
        Instant updatedAt,
        Instant lastDetectedAt
) {

    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getService(),
                incident.getStatus(),
                incident.getSeverity(),
                incident.getErrorRate(),
                incident.getStartTime(),
                incident.getEndTime(),
                incident.getSummary(),
                incident.getRootCause(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getLastDetectedAt()
        );
    }
}
