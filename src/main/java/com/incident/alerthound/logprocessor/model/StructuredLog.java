package com.incident.alerthound.logprocessor.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record StructuredLog(
        String id,
        String service,
        String level,
        String message,
        Instant timestamp,
        String traceId,
        String errorCategory,
        boolean error,
        Instant processedAt
) {
}
