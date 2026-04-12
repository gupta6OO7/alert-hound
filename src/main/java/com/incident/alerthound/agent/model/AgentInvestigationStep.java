package com.incident.alerthound.agent.model;

import java.util.List;
import java.util.Map;

public record AgentInvestigationStep(
        int iteration,
        AgentActionType actionType,
        String toolName,
        Map<String, Object> toolInput,
        Boolean toolSucceeded,
        String toolSummary,
        String finalSummary,
        String finalRootCause,
        List<String> recommendations
) {

    public static AgentInvestigationStep toolStep(
            int iteration,
            String toolName,
            Map<String, Object> toolInput,
            boolean toolSucceeded,
            String toolSummary
    ) {
        return new AgentInvestigationStep(
                iteration,
                AgentActionType.TOOL_CALL,
                toolName,
                toolInput,
                toolSucceeded,
                toolSummary,
                null,
                null,
                List.of()
        );
    }

    public static AgentInvestigationStep finalStep(
            int iteration,
            String finalSummary,
            String finalRootCause,
            List<String> recommendations
    ) {
        return new AgentInvestigationStep(
                iteration,
                AgentActionType.FINAL,
                null,
                Map.of(),
                null,
                null,
                finalSummary,
                finalRootCause,
                List.copyOf(recommendations)
        );
    }
}
