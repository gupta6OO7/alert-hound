package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.AgentActionType;
import com.incident.alerthound.agent.model.AgentContext;
import com.incident.alerthound.agent.model.AgentDecision;
import com.incident.alerthound.agent.model.ToolCall;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedLlmClient implements LlmClient {

    @Override
    public AgentDecision decide(AgentContext context) {
        if (!context.hasToolResult("search_logs")) {
            return AgentDecision.builder()
                    .actionType(AgentActionType.TOOL_CALL)
                    .toolCall(ToolCall.builder()
                            .name("search_logs")
                            .input(Map.of(
                                    "service", context.incident().service(),
                                    "from", context.incident().startTime().toString(),
                                    "to", context.incident().triggeredAt().toString(),
                                    "limit", 20
                            ))
                            .build())
                    .build();
        }

        if (!context.hasToolResult("get_incident_history")) {
            return AgentDecision.builder()
                    .actionType(AgentActionType.TOOL_CALL)
                    .toolCall(ToolCall.builder()
                            .name("get_incident_history")
                            .input(Map.of(
                                    "service", context.incident().service(),
                                    "limit", 5
                            ))
                            .build())
                    .build();
        }

        return AgentDecision.builder()
                .actionType(AgentActionType.FINAL)
                .summary(buildSummary(context))
                .rootCause(buildRootCause(context))
                .recommendations(buildRecommendations(context))
                .build();
    }

    private String buildSummary(AgentContext context) {
        int logCount = logsFromContext(context).size();
        return "Incident on service " + context.incident().service()
                + " reached " + String.format(Locale.ROOT, "%.2f%%", context.incident().errorRate() * 100.0d)
                + " error rate. Reviewed " + logCount + " recent error logs and "
                + context.historicalIncidents().size() + " historical incidents.";
    }

    private String buildRootCause(AgentContext context) {
        List<String> logs = logsFromContext(context);
        if (logs.stream().anyMatch(log -> log.toLowerCase(Locale.ROOT).contains("timeout"))) {
            return "Recent logs are dominated by timeout failures, suggesting a downstream dependency or database latency issue.";
        }
        if (logs.stream().anyMatch(log -> log.toLowerCase(Locale.ROOT).contains("connection"))) {
            return "Recent logs indicate connection-related failures, suggesting network instability or an unavailable upstream dependency.";
        }
        if (!context.historicalIncidents().isEmpty()) {
            return "The pattern resembles prior incidents for this service. Review similar historical incidents for recurring operational regressions.";
        }
        return "Unable to isolate a single root cause from the available logs. Additional metrics or deployment context is required.";
    }

    private List<String> buildRecommendations(AgentContext context) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Inspect the most recent deployment and infrastructure changes for service " + context.incident().service() + ".");

        List<String> logs = logsFromContext(context);
        if (logs.stream().anyMatch(log -> log.toLowerCase(Locale.ROOT).contains("timeout"))) {
            recommendations.add("Check downstream dependency latency, connection pool saturation, and database health.");
        }
        if (logs.stream().anyMatch(log -> log.toLowerCase(Locale.ROOT).contains("connection"))) {
            recommendations.add("Validate network reachability, DNS resolution, and upstream service availability.");
        }
        if (recommendations.size() == 1) {
            recommendations.add("Collect metrics and deployment metadata before attempting an automated rollback or restart.");
        }
        return List.copyOf(recommendations);
    }

    @SuppressWarnings("unchecked")
    private List<String> logsFromContext(AgentContext context) {
        return context.toolResults().stream()
                .filter(result -> "search_logs".equals(result.toolName()))
                .findFirst()
                .map(result -> (List<String>) result.payload().getOrDefault("logs", List.of()))
                .orElse(context.ragLogs());
    }
}
