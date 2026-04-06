package com.incident.alerthound.detection.service;

import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.detection.api.IncidentDebugResponse;
import com.incident.alerthound.detection.model.IncidentCreatedEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class IncidentDebugService {

    private final RedisLogCounterRepository logCounterRepository;
    private final DetectionThresholdEvaluator thresholdEvaluator;
    private final IncidentDedupRepository incidentDedupRepository;
    private final IncidentDebugRepository incidentDebugRepository;
    private final AlertHoundProperties.DetectionProperties properties;

    public IncidentDebugService(
            RedisLogCounterRepository logCounterRepository,
            DetectionThresholdEvaluator thresholdEvaluator,
            IncidentDedupRepository incidentDedupRepository,
            IncidentDebugRepository incidentDebugRepository,
            AlertHoundProperties alertHoundProperties
    ) {
        this.logCounterRepository = logCounterRepository;
        this.thresholdEvaluator = thresholdEvaluator;
        this.incidentDedupRepository = incidentDedupRepository;
        this.incidentDebugRepository = incidentDebugRepository;
        this.properties = alertHoundProperties.detection();
    }

    public IncidentDebugResponse inspect(String service, Instant requestedWindowEnd) {
        Instant windowEnd = requestedWindowEnd.truncatedTo(ChronoUnit.MINUTES);
        Instant windowStart = logCounterRepository.windowStart(windowEnd);
        DetectionSnapshot snapshot = logCounterRepository.readWindow(service, windowEnd);
        DetectionDecision decision = thresholdEvaluator.evaluate(snapshot, windowStart, windowEnd);
        IncidentCreatedEvent lastIncident = incidentDebugRepository.getLastIncident(service);

        return new IncidentDebugResponse(
                service,
                windowStart,
                windowEnd,
                snapshot.totalLogs(),
                snapshot.errorLogs(),
                snapshot.errorRate(),
                properties.minimumTotalLogs(),
                properties.errorRateThreshold(),
                decision.incidentTriggered(),
                decision.reason(),
                incidentDedupRepository.isActive(service),
                incidentDedupRepository.expiresAt(service),
                lastIncident
        );
    }
}
