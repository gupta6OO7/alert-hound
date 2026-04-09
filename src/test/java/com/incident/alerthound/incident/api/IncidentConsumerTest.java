package com.incident.alerthound.incident.api;

import com.incident.alerthound.detection.model.IncidentCreatedEvent;
import com.incident.alerthound.incident.service.IncidentService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidentConsumerTest {

    @Mock
    private IncidentService incidentService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldAcknowledgeAfterSuccessfulProcessing() {
        IncidentCreatedEvent event = incidentEvent();
        IncidentConsumer consumer = new IncidentConsumer(incidentService);

        consumer.consume(event, acknowledgment);

        verify(incidentService).handleIncidentCreated(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowFailuresForRetry() {
        IncidentCreatedEvent event = incidentEvent();
        IncidentConsumer consumer = new IncidentConsumer(incidentService);
        doThrow(new IllegalStateException("database unavailable")).when(incidentService).handleIncidentCreated(event);

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    private IncidentCreatedEvent incidentEvent() {
        UUID incidentId = UUID.randomUUID();
        return IncidentCreatedEvent.builder()
                .incidentId(incidentId.toString())
                .service("payment")
                .windowStart(Instant.parse("2026-04-07T10:00:00Z"))
                .windowEnd(Instant.parse("2026-04-07T10:01:00Z"))
                .triggeredAt(Instant.parse("2026-04-07T10:01:05Z"))
                .totalLogs(150)
                .errorLogs(18)
                .errorRate(0.12d)
                .severity("HIGH")
                .reason("threshold exceeded")
                .build();
    }
}
