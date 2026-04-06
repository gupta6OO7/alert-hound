package com.incident.alerthound.detection.service;

import com.incident.alerthound.logprocessor.model.StructuredLog;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectionService.class);

    private final RedisLogCounterRepository logCounterRepository;
    private final DetectionThresholdEvaluator thresholdEvaluator;
    private final IncidentDedupRepository incidentDedupRepository;
    private final IncidentPublisher incidentPublisher;

    public DetectionService(
            RedisLogCounterRepository logCounterRepository,
            DetectionThresholdEvaluator thresholdEvaluator,
            IncidentDedupRepository incidentDedupRepository,
            IncidentPublisher incidentPublisher
    ) {
        this.logCounterRepository = logCounterRepository;
        this.thresholdEvaluator = thresholdEvaluator;
        this.incidentDedupRepository = incidentDedupRepository;
        this.incidentPublisher = incidentPublisher;
    }

    public void process(StructuredLog log) {
        Instant timestamp = log.timestamp();
        String service = log.service();

        logCounterRepository.increment(timestamp, service, log.error());

        DetectionSnapshot snapshot = logCounterRepository.readWindow(service, timestamp);
        Instant windowEnd = timestamp.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        Instant windowStart = logCounterRepository.windowStart(windowEnd);
        DetectionDecision decision = thresholdEvaluator.evaluate(snapshot, windowStart, windowEnd);

        if (!decision.incidentTriggered()) {
            LOGGER.debug(
                    "Detection window for service {} did not trigger an incident. reason={}, totalLogs={}, errorLogs={}, errorRate={}",
                    service,
                    decision.reason(),
                    decision.totalLogs(),
                    decision.errorLogs(),
                    decision.errorRate()
            );
            return;
        }

        if (!incidentDedupRepository.tryActivate(service)) {
            LOGGER.debug("Skipping duplicate incident for service {}", service);
            return;
        }

        incidentPublisher.publish(service, decision);
    }
}
