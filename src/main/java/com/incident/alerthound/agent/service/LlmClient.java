package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.AgentContext;
import com.incident.alerthound.agent.model.AgentDecision;

public interface LlmClient {

    AgentDecision decide(AgentContext context);
}
