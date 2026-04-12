package com.incident.alerthound.agent.api;

import com.incident.alerthound.agent.model.AgentInvestigationReport;
import com.incident.alerthound.agent.service.AgentService;
import com.incident.alerthound.incident.model.AgentTaskEvent;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/debug")
public class AgentDebugController {

    private final AgentService agentService;

    public AgentDebugController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/investigate")
    public AgentInvestigationReport investigate(@Valid @RequestBody AgentDebugRequest request) {
        return agentService.inspect(AgentTaskEvent.builder()
                .incidentId(request.incidentId() != null ? request.incidentId() : UUID.randomUUID())
                .service(request.service().trim())
                .severity(request.severity().trim())
                .errorRate(request.errorRate())
                .startTime(request.startTime())
                .triggeredAt(request.triggeredAt())
                .build());
    }
}
