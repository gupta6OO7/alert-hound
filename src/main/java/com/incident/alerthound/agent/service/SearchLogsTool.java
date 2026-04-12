package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.ToolResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchLogsTool implements Tool {

    private final RagService ragService;

    public SearchLogsTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String getName() {
        return "search_logs";
    }

    @Override
    public ToolResult execute(Map<String, Object> input) {
        String service = stringValue(input.get("service"));
        Instant to = instantValue(input.get("to"), Instant.now());
        Instant from = instantValue(input.get("from"), to.minus(5, ChronoUnit.MINUTES));
        int limit = intValue(input.get("limit"), 20);
        return ragService.searchLogs(service, from, to, limit);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private Instant instantValue(Object value, Instant defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Instant.parse(value.toString());
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
