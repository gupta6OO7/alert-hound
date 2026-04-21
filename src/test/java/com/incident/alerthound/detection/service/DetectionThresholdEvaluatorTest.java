package com.incident.alerthound.detection.service;

import com.incident.alerthound.config.AlertHoundProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DetectionThresholdEvaluatorTest {

    private final DetectionThresholdEvaluator evaluator = new DetectionThresholdEvaluator(properties());

    @Test
    void shouldNotTriggerWhenTrafficIsBelowMinimumThreshold() {
        DetectionDecision decision = evaluator.evaluate(
                new DetectionSnapshot(99, 99),
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:01:00Z")
        );

        assertThat(decision.incidentTriggered()).isFalse();
        assertThat(decision.reason()).isEqualTo("traffic below minimum threshold");
    }

    @Test
    void shouldNotTriggerWhenErrorRateIsBelowThreshold() {
        DetectionDecision decision = evaluator.evaluate(
                new DetectionSnapshot(200, 8),
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:01:00Z")
        );

        assertThat(decision.incidentTriggered()).isFalse();
        assertThat(decision.reason()).isEqualTo("error rate below threshold");
    }

    @Test
    void shouldTriggerWhenThresholdIsExceeded() {
        DetectionDecision decision = evaluator.evaluate(
                new DetectionSnapshot(200, 15),
                Instant.parse("2026-04-06T10:00:00Z"),
                Instant.parse("2026-04-06T10:01:00Z")
        );

        assertThat(decision.incidentTriggered()).isTrue();
        assertThat(decision.errorRate()).isEqualTo(0.075d);
        assertThat(decision.reason()).contains("exceeded threshold");
    }

    private AlertHoundProperties properties() {
        return new AlertHoundProperties(
                null,
                null,
                new AlertHoundProperties.DetectionProperties("detection-group", 2, 10, 5, 100L, 0.05d),
                new AlertHoundProperties.IncidentProperties("incident-group", "incident-enrichment-group", 24),
                null
        );
    }
}
