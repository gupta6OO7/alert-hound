package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.ToolResult;
import java.util.Map;

public interface Tool {

    String getName();

    ToolResult execute(Map<String, Object> input);
}
