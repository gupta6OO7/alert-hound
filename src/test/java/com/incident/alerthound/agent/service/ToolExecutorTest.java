package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.ToolCall;
import com.incident.alerthound.agent.model.ToolExecutionException;
import com.incident.alerthound.agent.model.ToolResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolExecutorTest {

    @Test
    void shouldExecuteRegisteredTool() {
        Tool tool = new Tool() {
            @Override
            public String getName() {
                return "search_logs";
            }

            @Override
            public ToolResult execute(Map<String, Object> input) {
                return ToolResult.builder()
                        .toolName("search_logs")
                        .success(true)
                        .summary("ok")
                        .payload(Map.of("logs", List.of("a")))
                        .build();
            }
        };

        ToolExecutor executor = new ToolExecutor(List.of(tool));

        ToolResult result = executor.execute(ToolCall.builder().name("search_logs").input(Map.of()).build());

        assertThat(result.toolName()).isEqualTo("search_logs");
        assertThat(result.success()).isTrue();
    }

    @Test
    void shouldRejectUnknownTool() {
        ToolExecutor executor = new ToolExecutor(List.of());

        assertThatThrownBy(() -> executor.execute(ToolCall.builder().name("missing").input(Map.of()).build()))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessage("Unknown tool: missing");
    }
}
