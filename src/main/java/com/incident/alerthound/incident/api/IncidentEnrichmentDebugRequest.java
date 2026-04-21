package com.incident.alerthound.incident.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentEnrichmentDebugRequest(
        @NotNull UUID incidentId,
        @NotBlank String service,
        String summary,
        String rootCause,
        List<String> recommendations,
        Integer iterations,
        Boolean usedFallback,
        Instant completedAt
) {
}
