package com.incident.alerthound.logingestion.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogRequest(
        @NotBlank(message = "service is required")
        @Size(max = 100, message = "service must be at most 100 characters")
        String service,

        @Size(max = 20, message = "level must be at most 20 characters")
        String level,

        @NotBlank(message = "message is required")
        @Size(max = 10_000, message = "message must be at most 10000 characters")
        String message,

        String timestamp,
        String traceId
) {
}
