package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.ToolCall;
import com.incident.alerthound.agent.model.ToolExecutionException;
import com.incident.alerthound.agent.model.ToolResult;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ToolExecutor {

    private final Map<String, Tool> tools;

    public ToolExecutor(List<Tool> tools) {
        this.tools = tools.stream().collect(Collectors.toUnmodifiableMap(Tool::getName, Function.identity()));
    }

    public ToolResult execute(ToolCall call) {
        Tool tool = tools.get(call.name());
        if (tool == null) {
            throw new ToolExecutionException("Unknown tool: " + call.name());
        }
        return tool.execute(call.input());
    }
}
