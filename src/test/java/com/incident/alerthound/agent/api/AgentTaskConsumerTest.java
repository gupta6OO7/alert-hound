package com.incident.alerthound.agent.api;

import com.incident.alerthound.agent.service.AgentService;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.AgentTaskEvent;
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
class AgentTaskConsumerTest {

    @Mock
    private AgentService agentService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldAcknowledgeAfterSuccessfulProcessing() {
        AgentTaskConsumer consumer = new AgentTaskConsumer(agentService, properties());
        AgentTaskEvent event = event();

        consumer.consume(event, acknowledgment);

        verify(agentService).handle(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowFailuresForRetry() {
        AgentTaskConsumer consumer = new AgentTaskConsumer(agentService, properties());
        AgentTaskEvent event = event();
        doThrow(new IllegalStateException("llm unavailable")).when(agentService).handle(event);

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("llm unavailable");
    }

    private AlertHoundProperties properties() {
        return new AlertHoundProperties(
                new AlertHoundProperties.KafkaProperties(
                        null,
                        null,
                        null,
                        new AlertHoundProperties.TopicProperties(
                                null,
                                null,
                                null,
                                new AlertHoundProperties.TopicDefinition("agent.tasks", 3, (short) 1),
                                new AlertHoundProperties.TopicDefinition("agent.results", 3, (short) 1),
                                new AlertHoundProperties.TopicDefinition("incidents.updated", 3, (short) 1)
                        )
                ),
                null,
                null,
                new AlertHoundProperties.IncidentProperties("incident-group", "incident-enrichment-group", 24),
                new AlertHoundProperties.AgentProperties("agent-group", 3, 20, 5, 5)
        );
    }

    private AgentTaskEvent event() {
        return AgentTaskEvent.builder()
                .incidentId(UUID.randomUUID())
                .service("payment")
                .severity("HIGH")
                .errorRate(0.20d)
                .startTime(Instant.parse("2026-04-09T10:25:00Z"))
                .triggeredAt(Instant.parse("2026-04-09T10:30:00Z"))
                .build();
    }
}
