package com.incident.alerthound.incident.api;

import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.model.IncidentStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
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
        List<String> recommendations,
        Instant createdAt,
        Instant updatedAt,
        Instant lastDetectedAt
) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                parseRecommendations(incident.getRecommendationsJson()),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getLastDetectedAt()
        );
    }

    private static List<String> parseRecommendations(String recommendationsJson) {
        if (recommendationsJson == null || recommendationsJson.isBlank()) {
            return List.of();
        }

        try {
            return OBJECT_MAPPER.readValue(recommendationsJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
