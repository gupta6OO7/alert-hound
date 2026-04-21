package com.incident.alerthound.incident.api;

import com.incident.alerthound.agent.model.AgentResultEvent;
import com.incident.alerthound.incident.service.IncidentEnrichmentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/incidents/debug")
public class IncidentEnrichmentDebugController {

    private final IncidentEnrichmentService incidentEnrichmentService;

    public IncidentEnrichmentDebugController(IncidentEnrichmentService incidentEnrichmentService) {
        this.incidentEnrichmentService = incidentEnrichmentService;
    }

    @PostMapping("/enrich")
    public IncidentEnrichmentService.IncidentEnrichmentReport enrich(@Valid @RequestBody IncidentEnrichmentDebugRequest request) {
        return incidentEnrichmentService.inspect(AgentResultEvent.builder()
                .incidentId(request.incidentId())
                .service(request.service().trim())
                .summary(request.summary())
                .rootCause(request.rootCause())
                .recommendations(request.recommendations() == null ? List.of() : List.copyOf(request.recommendations()))
                .iterations(request.iterations() == null ? 0 : request.iterations())
                .usedFallback(Boolean.TRUE.equals(request.usedFallback()))
                .completedAt(request.completedAt() == null ? Instant.now() : request.completedAt())
                .build());
    }
}
