package com.incident.alerthound.agent.model;

import java.util.List;
import lombok.Builder;

@Builder
public record AgentDecision(
        AgentActionType actionType,
        ToolCall toolCall,
        String summary,
        String rootCause,
        List<String> recommendations
) {
}
