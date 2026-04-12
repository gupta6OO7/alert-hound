package com.incident.alerthound.agent.model;

import com.incident.alerthound.incident.model.AgentTaskEvent;
import java.util.ArrayList;
import java.util.List;

public class AgentContext {

    private final AgentTaskEvent incident;
    private final List<String> ragLogs;
    private final List<String> historicalIncidents;
    private final List<ToolResult> toolResults = new ArrayList<>();

    public AgentContext(AgentTaskEvent incident, List<String> ragLogs, List<String> historicalIncidents) {
        this.incident = incident;
        this.ragLogs = new ArrayList<>(ragLogs);
        this.historicalIncidents = new ArrayList<>(historicalIncidents);
    }

    public AgentTaskEvent incident() {
        return incident;
    }

    public List<String> ragLogs() {
        return List.copyOf(ragLogs);
    }

    public List<String> historicalIncidents() {
        return List.copyOf(historicalIncidents);
    }

    public List<ToolResult> toolResults() {
        return List.copyOf(toolResults);
    }

    public void addToolResult(ToolResult toolResult) {
        this.toolResults.add(toolResult);
    }

    public boolean hasToolResult(String toolName) {
        return toolResults.stream().anyMatch(result -> result.toolName().equals(toolName));
    }
}
