package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.ToolCall;
import com.incident.alerthound.agent.model.ToolExecutionException;
import com.incident.alerthound.agent.model.ToolResult;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ToolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolExecutor.class);

    private final Map<String, Tool> tools;

    public ToolExecutor(List<Tool> tools) {
        this.tools = tools.stream().collect(Collectors.toUnmodifiableMap(Tool::getName, Function.identity()));
    }

    public ToolResult execute(ToolCall call) {
        Tool tool = tools.get(call.name());
        if (tool == null) {
            throw new ToolExecutionException("Unknown tool: " + call.name());
        }
        LOGGER.debug("Dispatching tool name={} input={}", call.name(), call.input());
        ToolResult result = tool.execute(call.input());
        LOGGER.debug("Tool returned name={} success={} summary={}", result.toolName(), result.success(), result.summary());
        return result;
    }
}
