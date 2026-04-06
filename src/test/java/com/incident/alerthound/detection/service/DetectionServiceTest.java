package com.incident.alerthound.detection.service;

import com.incident.alerthound.logprocessor.model.StructuredLog;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionServiceTest {

    @Mock
    private RedisLogCounterRepository logCounterRepository;

    @Mock
    private DetectionThresholdEvaluator thresholdEvaluator;

    @Mock
    private IncidentDedupRepository incidentDedupRepository;

    @Mock
    private IncidentPublisher incidentPublisher;

    @InjectMocks
    private DetectionService detectionService;

    @Test
    void shouldPublishIncidentWhenThresholdExceededAndDedupAllowsIt() {
        StructuredLog log = log();
        Instant windowStart = Instant.parse("2026-04-06T10:00:00Z");
        Instant windowEnd = Instant.parse("2026-04-06T10:01:00Z");
        DetectionDecision decision = DetectionDecision.incident(200, 20, 0.10d, windowStart, windowEnd, "threshold exceeded");

        when(logCounterRepository.readWindow("payment", Instant.parse("2026-04-06T10:01:10Z")))
                .thenReturn(new DetectionSnapshot(200, 20));
        when(logCounterRepository.windowStart(windowEnd)).thenReturn(windowStart);
        when(thresholdEvaluator.evaluate(new DetectionSnapshot(200, 20), windowStart, windowEnd)).thenReturn(decision);
        when(incidentDedupRepository.tryActivate("payment")).thenReturn(true);

        detectionService.process(log);

        verify(logCounterRepository).increment(Instant.parse("2026-04-06T10:01:10Z"), "payment", true);
        verify(incidentPublisher).publish("payment", decision);
    }

    @Test
    void shouldSuppressDuplicateIncidentWhenDedupKeyExists() {
        StructuredLog log = log();
        Instant windowStart = Instant.parse("2026-04-06T10:00:00Z");
        Instant windowEnd = Instant.parse("2026-04-06T10:01:00Z");
        DetectionDecision decision = DetectionDecision.incident(200, 20, 0.10d, windowStart, windowEnd, "threshold exceeded");

        when(logCounterRepository.readWindow("payment", Instant.parse("2026-04-06T10:01:10Z")))
                .thenReturn(new DetectionSnapshot(200, 20));
        when(logCounterRepository.windowStart(windowEnd)).thenReturn(windowStart);
        when(thresholdEvaluator.evaluate(new DetectionSnapshot(200, 20), windowStart, windowEnd)).thenReturn(decision);
        when(incidentDedupRepository.tryActivate("payment")).thenReturn(false);

        detectionService.process(log);

        verify(incidentPublisher, never()).publish("payment", decision);
    }

    @Test
    void shouldSkipPublishingWhenNoIncidentIsDetected() {
        StructuredLog log = log();
        Instant windowStart = Instant.parse("2026-04-06T10:00:00Z");
        Instant windowEnd = Instant.parse("2026-04-06T10:01:00Z");
        DetectionDecision decision = DetectionDecision.noIncident(200, 5, 0.025d, windowStart, windowEnd, "below threshold");

        when(logCounterRepository.readWindow("payment", Instant.parse("2026-04-06T10:01:10Z")))
                .thenReturn(new DetectionSnapshot(200, 5));
        when(logCounterRepository.windowStart(windowEnd)).thenReturn(windowStart);
        when(thresholdEvaluator.evaluate(new DetectionSnapshot(200, 5), windowStart, windowEnd)).thenReturn(decision);

        detectionService.process(log);

        verify(incidentDedupRepository, never()).tryActivate("payment");
        verify(incidentPublisher, never()).publish("payment", decision);
    }

    private StructuredLog log() {
        return StructuredLog.builder()
                .id("log-1")
                .service("payment")
                .level("ERROR")
                .message("timeout")
                .timestamp(Instant.parse("2026-04-06T10:01:10Z"))
                .traceId("trace-1")
                .errorCategory("TIMEOUT")
                .error(true)
                .processedAt(Instant.parse("2026-04-06T10:01:11Z"))
                .build();
    }
}
