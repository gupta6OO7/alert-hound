package com.incident.alerthound.incident.api;

import com.incident.alerthound.agent.model.AgentResultEvent;
import com.incident.alerthound.incident.service.IncidentEnrichmentService;
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
class AgentResultConsumerTest {

    @Mock
    private IncidentEnrichmentService incidentEnrichmentService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldAcknowledgeAfterSuccessfulEnrichment() {
        AgentResultConsumer consumer = new AgentResultConsumer(incidentEnrichmentService);
        AgentResultEvent event = event();

        consumer.consume(event, acknowledgment);

        verify(incidentEnrichmentService).process(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowFailuresForRetry() {
        AgentResultConsumer consumer = new AgentResultConsumer(incidentEnrichmentService);
        AgentResultEvent event = event();
        doThrow(new IllegalStateException("database unavailable")).when(incidentEnrichmentService).process(event);

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    private AgentResultEvent event() {
        return AgentResultEvent.builder()
                .incidentId(UUID.randomUUID())
                .service("payment")
                .summary("summary")
                .rootCause("root")
                .recommendations(List.of("check db"))
                .iterations(3)
                .usedFallback(false)
                .completedAt(Instant.parse("2026-04-12T09:31:00Z"))
                .build();
    }
}
