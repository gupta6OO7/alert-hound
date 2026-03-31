package com.incident.alerthound.logingestion.model;

import lombok.Builder;

@Builder
public record LogEvent(
        String id,
        String service,
        String level,
        String message,
        String timestamp,
        String traceId
) {
}
