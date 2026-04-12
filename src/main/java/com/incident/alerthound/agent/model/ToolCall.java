package com.incident.alerthound.agent.model;

import java.util.Map;
import lombok.Builder;

@Builder
public record ToolCall(
        String name,
        Map<String, Object> input
) {
}
