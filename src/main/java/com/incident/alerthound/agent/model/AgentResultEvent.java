package com.incident.alerthound.agent.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AgentResultEvent(
        UUID incidentId,
        String service,
        String summary,
        String rootCause,
        List<String> recommendations,
        int iterations,
        boolean usedFallback,
        Instant completedAt
) {
}
