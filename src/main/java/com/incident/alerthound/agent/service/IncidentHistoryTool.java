package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.ToolResult;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class IncidentHistoryTool implements Tool {

    private final RagService ragService;

    public IncidentHistoryTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String getName() {
        return "get_incident_history";
    }

    @Override
    public ToolResult execute(Map<String, Object> input) {
        String service = input.get("service") == null ? "" : input.get("service").toString();
        int limit = input.get("limit") instanceof Number number ? number.intValue() : 5;
        return ragService.fetchIncidentHistory(service, limit);
    }
}
