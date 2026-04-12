package com.incident.alerthound.agent.model;

import java.util.List;

public record AgentInvestigationReport(
        String reasoningClient,
        String reasoningModel,
        int maxIterations,
        List<String> ragLogs,
        List<String> historicalIncidents,
        List<AgentInvestigationStep> steps,
        AgentResultEvent result
) {
}
