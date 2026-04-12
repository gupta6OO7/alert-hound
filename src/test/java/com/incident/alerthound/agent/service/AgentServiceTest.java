package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.AgentActionType;
import com.incident.alerthound.agent.model.AgentContext;
import com.incident.alerthound.agent.model.AgentDecision;
import com.incident.alerthound.agent.model.ToolCall;
import com.incident.alerthound.agent.model.ToolResult;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.AgentTaskEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private ToolExecutor toolExecutor;

    @Mock
    private RagService ragService;

    @Mock
    private LlmClient llmClient;

    @Mock
    private AgentResultProducer agentResultProducer;

    @Test
    void shouldIterateToolsAndPublishFinalResult() {
        AgentService agentService = new AgentService(toolExecutor, ragService, llmClient, agentResultProducer, properties(3));
        AgentTaskEvent task = task();
        AgentContext context = new AgentContext(task, List.of("timeout"), List.of("similar incident"));
        ToolResult toolResult = ToolResult.builder()
                .toolName("search_logs")
                .success(true)
                .summary("ok")
                .payload(Map.of("logs", List.of("timeout database")))
                .build();

        when(ragService.initializeContext(task)).thenReturn(context);
        when(llmClient.decide(context))
                .thenReturn(AgentDecision.builder()
                        .actionType(AgentActionType.TOOL_CALL)
                        .toolCall(ToolCall.builder().name("search_logs").input(Map.of()).build())
                        .build())
                .thenReturn(AgentDecision.builder()
                        .actionType(AgentActionType.FINAL)
                        .summary("summary")
                        .rootCause("root cause")
                        .recommendations(List.of("check db"))
                        .build());
        when(toolExecutor.execute(org.mockito.ArgumentMatchers.any())).thenReturn(toolResult);

        var result = agentService.handle(task);

        assertThat(result.usedFallback()).isFalse();
        assertThat(result.iterations()).isEqualTo(2);
        verify(agentResultProducer).publish(result);
    }

    @Test
    void shouldFallbackAfterMaxIterations() {
        AgentService agentService = new AgentService(toolExecutor, ragService, llmClient, agentResultProducer, properties(2));
        AgentTaskEvent task = task();
        AgentContext context = new AgentContext(task, List.of(), List.of());

        when(ragService.initializeContext(task)).thenReturn(context);
        when(llmClient.decide(context))
                .thenReturn(AgentDecision.builder()
                        .actionType(AgentActionType.TOOL_CALL)
                        .toolCall(ToolCall.builder().name("search_logs").input(Map.of()).build())
                        .build())
                .thenReturn(AgentDecision.builder()
                        .actionType(AgentActionType.TOOL_CALL)
                        .toolCall(ToolCall.builder().name("get_incident_history").input(Map.of()).build())
                        .build());
        doThrow(new IllegalStateException("boom")).when(toolExecutor).execute(org.mockito.ArgumentMatchers.any());

        var result = agentService.handle(task);

        assertThat(result.usedFallback()).isTrue();
        assertThat(result.iterations()).isEqualTo(2);
        verify(agentResultProducer).publish(result);
    }

    private AlertHoundProperties properties(int maxIterations) {
        return new AlertHoundProperties(
                null,
                null,
                null,
                null,
                new AlertHoundProperties.AgentProperties("agent-group", maxIterations, 20, 5, 5)
        );
    }

    private AgentTaskEvent task() {
        return AgentTaskEvent.builder()
                .incidentId(UUID.randomUUID())
                .service("payment")
                .severity("CRITICAL")
                .errorRate(0.75d)
                .startTime(Instant.parse("2026-04-09T10:25:00Z"))
                .triggeredAt(Instant.parse("2026-04-09T10:30:00Z"))
                .build();
    }
}
