package com.incident.alerthound.incident.api;

import com.incident.alerthound.incident.service.IncidentService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/active")
    public List<IncidentResponse> getActiveIncidents() {
        return incidentService.getActiveIncidents().stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncident(@PathVariable UUID id) {
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<IncidentResponse> resolveIncident(@PathVariable UUID id) {
        return ResponseEntity.ok(IncidentResponse.from(incidentService.resolveIncident(id)));
    }
}
