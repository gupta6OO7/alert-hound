package com.incident.alerthound.incident.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AgentTaskEvent(
        UUID incidentId,
        String service,
        String severity,
        double errorRate,
        Instant startTime,
        Instant triggeredAt
) {
}
