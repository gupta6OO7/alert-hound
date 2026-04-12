package com.incident.alerthound.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.agent.model.AgentActionType;
import com.incident.alerthound.agent.model.AgentContext;
import com.incident.alerthound.agent.model.AgentDecision;
import com.incident.alerthound.agent.model.ToolCall;
import com.incident.alerthound.agent.model.ToolResult;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@Primary
@ConditionalOnExpression("'${spring.ai.google.genai.api-key:${GEMINI_API_KEY:}}' != ''")
public class GeminiLlmClient implements LlmClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MILLIS = 750L;
    private static final String SYSTEM_PROMPT = """
            You are a senior SRE incident investigation agent.
            Respond with JSON only. Do not include markdown fences.
            You must choose one of two actions:
            1. TOOL_CALL
            2. FINAL

            Allowed tools:
            - search_logs
            - get_incident_history

            JSON schema for TOOL_CALL:
            {
              "actionType": "TOOL_CALL",
              "toolCall": {
                "name": "search_logs" | "get_incident_history",
                "input": { ... }
              }
            }

            JSON schema for FINAL:
            {
              "actionType": "FINAL",
              "summary": "short incident summary",
              "rootCause": "best current root cause hypothesis",
              "recommendations": ["action 1", "action 2"]
            }

            Accepted aliases if you cannot follow the schema exactly:
            - "next_action" or "tool" for the requested tool name
            - "parameters" instead of "toolCall.input"
            - "fetch_logs" means "search_logs"
            - "incident_history" means "get_incident_history"

            Rules:
            - Use tools only when they add evidence you do not already have.
            - If the available evidence is sufficient, return FINAL immediately.
            - Be concise, evidence-based, and operationally useful.
            - Never invent deployments, metrics, or logs that are not present in the context.
            - Recommendations must be concrete and safe.
            - Never request tools other than search_logs or get_incident_history.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RuleBasedLlmClient fallbackClient;
    private final String model;

    public GeminiLlmClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            RuleBasedLlmClient fallbackClient,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.google.genai.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.google.genai.model:${GEMINI_MODEL:gemini-2.5-flash}}") String model
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.fallbackClient = fallbackClient;
        this.model = model;
        LOGGER.info("GeminiLlmClient enabled with model {}", model);
    }

    @Override
    public AgentDecision decide(AgentContext context) {
        try {
            LOGGER.debug("Calling Gemini model {} for incident {}", model, context.incident().incidentId());
            String response = extractText(callGemini(buildPrompt(context)));

            if (response == null || response.isBlank()) {
                LOGGER.warn("Gemini returned an empty response. Falling back to rule-based decision.");
                return fallbackClient.decide(context);
            }

            return parseDecision(response, context);
        } catch (RuntimeException exception) {
            LOGGER.warn("Gemini decision failed. Falling back to rule-based decision.", exception);
            return fallbackClient.decide(context);
        }
    }

    private JsonNode callGemini(String prompt) {
        Map<String, Object> request = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json"
                )
        );

        long retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return restClient.post()
                        .uri("/v1beta/models/{model}:generateContent", model)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientResponseException exception) {
                if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
                    throw exception;
                }

                LOGGER.warn(
                        "Gemini request attempt {} of {} failed with status {}. Retrying in {} ms.",
                        attempt,
                        MAX_ATTEMPTS,
                        exception.getStatusCode().value(),
                        retryDelayMillis
                );
                sleep(retryDelayMillis);
                retryDelayMillis *= 2;
            }
        }

        throw new IllegalStateException("Gemini request exhausted retry attempts");
    }

    private String buildPrompt(AgentContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incident", Map.of(
                "incidentId", context.incident().incidentId(),
                "service", context.incident().service(),
                "severity", context.incident().severity(),
                "errorRate", String.format(Locale.ROOT, "%.4f", context.incident().errorRate()),
                "startTime", stringValue(context.incident().startTime()),
                "triggeredAt", stringValue(context.incident().triggeredAt())
        ));
        payload.put("ragLogs", context.ragLogs());
        payload.put("historicalIncidents", context.historicalIncidents());
        payload.put("toolResults", context.toolResults().stream().map(this::serializeToolResult).toList());
        payload.put("instruction", "Choose the next best action for this incident.");

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Gemini prompt payload", exception);
        }
    }

    private Map<String, Object> serializeToolResult(ToolResult result) {
        return Map.of(
                "toolName", result.toolName(),
                "success", result.success(),
                "summary", result.summary(),
                "payload", result.payload()
        );
    }

    private AgentDecision parseDecision(String response, AgentContext context) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFences(response));
            String actionType = root.path("actionType").asText("");

            if ("TOOL_CALL".equalsIgnoreCase(actionType)) {
                AgentDecision toolDecision = parseToolDecision(root);
                if (toolDecision != null) {
                    return toolDecision;
                }
                LOGGER.warn("Gemini returned TOOL_CALL without a supported tool name. Falling back to rule-based decision.");
                return fallbackClient.decide(context);
            }

            if ("FINAL".equalsIgnoreCase(actionType)) {
                List<String> recommendations = root.path("recommendations").isArray()
                        ? objectMapper.convertValue(
                                root.path("recommendations"),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                        )
                        : List.of();

                return AgentDecision.builder()
                        .actionType(AgentActionType.FINAL)
                        .summary(root.path("summary").asText("Incident investigation completed."))
                        .rootCause(root.path("rootCause").asText("Unable to determine root cause."))
                        .recommendations(recommendations.isEmpty()
                                ? List.of("Review logs, recent deployments, and service health for additional context.")
                                : recommendations)
                        .build();
            }

            AgentDecision aliasedToolDecision = parseAliasedToolDecision(root);
            if (aliasedToolDecision != null) {
                return aliasedToolDecision;
            }
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Failed to parse Gemini response as JSON. Falling back to rule-based decision. response={}", response, exception);
            return fallbackClient.decide(context);
        }

        LOGGER.warn("Gemini returned an unsupported action. Falling back to rule-based decision. response={}", response);
        return fallbackClient.decide(context);
    }

    private String stripCodeFences(String response) {
        String trimmed = response.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstNewline = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewline < 0 || lastFence <= firstNewline) {
            return trimmed;
        }
        return trimmed.substring(firstNewline + 1, lastFence).trim();
    }

    private AgentDecision parseToolDecision(JsonNode root) {
        JsonNode toolCall = root.path("toolCall");
        String toolName = normalizeToolName(toolCall.path("name").asText(""));
        if (toolName == null) {
            return null;
        }

        Map<String, Object> input = mapValue(toolCall.path("input"));
        return AgentDecision.builder()
                .actionType(AgentActionType.TOOL_CALL)
                .toolCall(ToolCall.builder()
                        .name(toolName)
                        .input(input)
                        .build())
                .build();
    }

    private AgentDecision parseAliasedToolDecision(JsonNode root) {
        String toolName = normalizeToolName(firstNonBlank(
                root.path("tool").asText(""),
                root.path("next_action").asText("")
        ));
        if (toolName == null) {
            return null;
        }

        Map<String, Object> input = mapValue(root.path("parameters"));
        input = normalizeToolInput(toolName, input);
        LOGGER.info("Accepted Gemini tool alias '{}' as '{}'", firstNonBlank(
                root.path("tool").asText(""),
                root.path("next_action").asText("")
        ), toolName);

        return AgentDecision.builder()
                .actionType(AgentActionType.TOOL_CALL)
                .toolCall(ToolCall.builder()
                        .name(toolName)
                        .input(input)
                        .build())
                .build();
    }

    private Map<String, Object> mapValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        Map<String, Object> input = objectMapper.convertValue(
                node,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
        );
        return input == null ? Map.of() : input;
    }

    private String normalizeToolName(String rawToolName) {
        String normalized = rawToolName == null ? "" : rawToolName.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "search_logs", "fetch_logs" -> "search_logs";
            case "get_incident_history", "incident_history", "fetch_incident_history" -> "get_incident_history";
            default -> null;
        };
    }

    private Map<String, Object> normalizeToolInput(String toolName, Map<String, Object> input) {
        if (input.isEmpty()) {
            return input;
        }

        Map<String, Object> normalized = new LinkedHashMap<>(input);
        if ("search_logs".equals(toolName)) {
            renameKey(normalized, "start_time", "from");
            renameKey(normalized, "end_time", "to");
        }
        return Map.copyOf(normalized);
    }

    private void renameKey(Map<String, Object> input, String sourceKey, String targetKey) {
        Object value = input.remove(sourceKey);
        if (value != null && !input.containsKey(targetKey)) {
            input.put(targetKey, value);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Instant value) {
        return value == null ? "" : value.toString();
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode parts = response.path("candidates");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }

        JsonNode textParts = parts.get(0).path("content").path("parts");
        if (!textParts.isArray() || textParts.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode part : textParts) {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(text);
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private boolean isRetryable(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        return status == 429 || status == 503;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry Gemini request", exception);
        }
    }

    public String modelName() {
        return model;
    }
}
