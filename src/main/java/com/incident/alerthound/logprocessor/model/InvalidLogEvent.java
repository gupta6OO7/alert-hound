package com.incident.alerthound.logprocessor.model;

import com.incident.alerthound.logingestion.model.LogEvent;
import java.time.Instant;
import lombok.Builder;

@Builder
public record InvalidLogEvent(
        LogEvent originalEvent,
        String reason,
        Instant failedAt
) {
}
