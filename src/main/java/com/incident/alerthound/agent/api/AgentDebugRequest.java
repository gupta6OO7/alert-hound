package com.incident.alerthound.agent.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AgentDebugRequest(
        UUID incidentId,
        @NotBlank String service,
        @NotBlank String severity,
        @DecimalMin("0.0") @DecimalMax("1.0") double errorRate,
        @NotNull Instant startTime,
        @NotNull Instant triggeredAt
) {
}
