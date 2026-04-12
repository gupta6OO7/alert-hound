package com.incident.alerthound.agent.model;

import java.util.Map;
import lombok.Builder;

@Builder
public record ToolResult(
        String toolName,
        boolean success,
        String summary,
        Map<String, Object> payload
) {
}
