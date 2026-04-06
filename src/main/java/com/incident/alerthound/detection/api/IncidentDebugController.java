package com.incident.alerthound.detection.api;

import com.incident.alerthound.detection.service.IncidentDebugService;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/incidents")
public class IncidentDebugController {

    private final IncidentDebugService incidentDebugService;

    public IncidentDebugController(IncidentDebugService incidentDebugService) {
        this.incidentDebugService = incidentDebugService;
    }

    @GetMapping("/debug/{service}")
    public IncidentDebugResponse inspect(
            @PathVariable String service,
            @RequestParam(name = "windowEnd", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant windowEnd
    ) {
        return incidentDebugService.inspect(service, windowEnd != null ? windowEnd : Instant.now());
    }
}
