package com.incident.alerthound.consumerfacing.api;

import com.incident.alerthound.consumerfacing.service.ConsumerIncidentViewService;
import com.incident.alerthound.incident.model.IncidentStatus;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import java.time.Instant;
import java.util.List;
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
class IncidentUpdatedViewConsumerTest {

    @Mock
    private ConsumerIncidentViewService consumerIncidentViewService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldAcknowledgeAfterSuccessfulViewUpdate() {
        IncidentUpdatedViewConsumer consumer = new IncidentUpdatedViewConsumer(consumerIncidentViewService);
        IncidentUpdatedEvent event = event();

        consumer.consume(event, acknowledgment);

        verify(consumerIncidentViewService).apply(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowFailuresForRetry() {
        IncidentUpdatedViewConsumer consumer = new IncidentUpdatedViewConsumer(consumerIncidentViewService);
        IncidentUpdatedEvent event = event();
        doThrow(new IllegalStateException("view store unavailable")).when(consumerIncidentViewService).apply(event);

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("view store unavailable");
    }

    private IncidentUpdatedEvent event() {
        return IncidentUpdatedEvent.builder()
                .incidentId(UUID.randomUUID())
                .service("payment")
                .status(IncidentStatus.INVESTIGATED)
                .severity("CRITICAL")
                .errorRate(1.0d)
                .summary("summary")
                .rootCause("root cause")
                .recommendations(List.of("restart"))
                .updatedAt(Instant.parse("2026-04-22T00:17:55Z"))
                .lastDetectedAt(Instant.parse("2026-04-22T00:17:03Z"))
                .build();
    }
}
