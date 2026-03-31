package com.incident.alerthound.logingestion.api;

import com.incident.alerthound.logingestion.service.LogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping
    public ResponseEntity<LogAcceptedResponse> ingest(@Valid @RequestBody LogRequest request) {
        String logId = logService.processLog(request);
        return ResponseEntity.accepted().body(new LogAcceptedResponse("accepted", logId));
    }
}
