package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.AgentContext;
import com.incident.alerthound.agent.model.ToolResult;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.AgentTaskEvent;
import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.repository.IncidentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final IncidentRepository incidentRepository;
    private final AgentLogSearchService agentLogSearchService;
    private final int recentLogsLimit;
    private final int historyLimit;
    private final int logLookbackMinutes;

    public RagService(
            IncidentRepository incidentRepository,
            AgentLogSearchService agentLogSearchService,
            AlertHoundProperties properties
    ) {
        this.incidentRepository = incidentRepository;
        this.agentLogSearchService = agentLogSearchService;
        this.recentLogsLimit = properties != null && properties.agent() != null && properties.agent().recentLogsLimit() > 0
                ? properties.agent().recentLogsLimit()
                : 20;
        this.historyLimit = properties != null && properties.agent() != null && properties.agent().historyLimit() > 0
                ? properties.agent().historyLimit()
                : 5;
        this.logLookbackMinutes = properties != null && properties.agent() != null && properties.agent().logLookbackMinutes() > 0
                ? properties.agent().logLookbackMinutes()
                : 5;
    }

    public AgentContext initializeContext(AgentTaskEvent task) {
        Instant windowStart = task.startTime() != null
                ? task.startTime()
                : task.triggeredAt().minus(logLookbackMinutes, ChronoUnit.MINUTES);

        List<String> logs = agentLogSearchService.searchRecentLogs(task.service(), windowStart, task.triggeredAt(), recentLogsLimit);
        List<String> history = incidentRepository.findTop5ByServiceOrderByUpdatedAtDesc(task.service()).stream()
                .filter(incident -> !incident.getId().equals(task.incidentId()))
                .limit(historyLimit)
                .map(this::toHistorySnippet)
                .toList();

        return new AgentContext(task, logs, history);
    }

    public ToolResult searchLogs(String service, Instant from, Instant to, int limit) {
        List<String> logs = agentLogSearchService.searchRecentLogs(service, from, to, limit);
        return ToolResult.builder()
                .toolName("search_logs")
                .success(true)
                .summary("Fetched " + logs.size() + " recent logs")
                .payload(Map.of("logs", logs))
                .build();
    }

    public ToolResult fetchIncidentHistory(String service, int limit) {
        List<String> history = incidentRepository.findTop5ByServiceOrderByUpdatedAtDesc(service).stream()
                .limit(limit)
                .map(this::toHistorySnippet)
                .collect(Collectors.toList());

        return ToolResult.builder()
                .toolName("get_incident_history")
                .success(true)
                .summary("Fetched " + history.size() + " historical incidents")
                .payload(Map.of("history", history))
                .build();
    }

    private String toHistorySnippet(Incident incident) {
        return "severity=" + incident.getSeverity()
                + ", status=" + incident.getStatus()
                + ", errorRate=" + String.format(Locale.ROOT, "%.2f%%", incident.getErrorRate() * 100.0d)
                + ", summary=" + sanitize(incident.getSummary())
                + ", rootCause=" + sanitize(incident.getRootCause());
    }

    private String sanitize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
