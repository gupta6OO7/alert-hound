package com.incident.alerthound.incident.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.agent.model.AgentResultEvent;
import com.incident.alerthound.incident.model.ActiveIncidentState;
import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.model.IncidentStatus;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import com.incident.alerthound.incident.repository.IncidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentEnrichmentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ActiveIncidentCacheRepository activeIncidentCacheRepository;

    @Mock
    private IncidentUpdateProducer incidentUpdateProducer;

    @Test
    void shouldUpdateIncidentCacheAndPublishUpdate() {
        IncidentEnrichmentService service = new IncidentEnrichmentService(
                incidentRepository,
                activeIncidentCacheRepository,
                incidentUpdateProducer,
                new ObjectMapper()
        );
        Incident incident = incident();
        AgentResultEvent result = result(incident.getId());
        IncidentUpdatedEvent updateEvent = IncidentUpdatedEvent.builder()
                .incidentId(incident.getId())
                .service("payment")
                .status(IncidentStatus.INVESTIGATED)
                .severity("CRITICAL")
                .errorRate(1.0d)
                .summary(result.summary())
                .rootCause(result.rootCause())
                .recommendations(result.recommendations())
                .updatedAt(Instant.parse("2026-04-12T09:31:00Z"))
                .lastDetectedAt(incident.getLastDetectedAt())
                .build();

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenAnswer(invocation -> {
            Incident saved = invocation.getArgument(0);
            saved.setUpdatedAt(Instant.parse("2026-04-12T09:31:00Z"));
            return saved;
        });
        when(incidentUpdateProducer.publish(incident)).thenReturn(updateEvent);

        IncidentEnrichmentService.IncidentEnrichmentReport report = service.process(result);

        assertThat(report.updated()).isTrue();
        assertThat(report.incident().status()).isEqualTo(IncidentStatus.INVESTIGATED);
        assertThat(report.incident().summary()).isEqualTo(result.summary());
        assertThat(report.incident().recommendations()).containsExactlyElementsOf(result.recommendations());
        verify(activeIncidentCacheRepository).save(org.mockito.ArgumentMatchers.any(ActiveIncidentState.class));
        verify(incidentUpdateProducer).publish(incident);
    }

    @Test
    void shouldIgnoreResolvedIncident() {
        IncidentEnrichmentService service = new IncidentEnrichmentService(
                incidentRepository,
                activeIncidentCacheRepository,
                incidentUpdateProducer,
                new ObjectMapper()
        );
        Incident incident = incident();
        incident.setStatus(IncidentStatus.RESOLVED);
        AgentResultEvent result = result(incident.getId());

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        IncidentEnrichmentService.IncidentEnrichmentReport report = service.process(result);

        assertThat(report.ignored()).isTrue();
        assertThat(report.reason()).isEqualTo("incident already resolved");
        verify(incidentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(incidentUpdateProducer, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private Incident incident() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setService("payment");
        incident.setStatus(IncidentStatus.INVESTIGATING);
        incident.setSeverity("CRITICAL");
        incident.setErrorRate(1.0d);
        incident.setStartTime(Instant.parse("2026-04-12T09:29:00Z"));
        incident.setLastDetectedAt(Instant.parse("2026-04-12T09:30:00Z"));
        incident.setCreatedAt(Instant.parse("2026-04-12T09:30:00Z"));
        incident.setUpdatedAt(Instant.parse("2026-04-12T09:30:00Z"));
        return incident;
    }

    private AgentResultEvent result(UUID incidentId) {
        return AgentResultEvent.builder()
                .incidentId(incidentId)
                .service("payment")
                .summary("Payment failures caused by database timeouts.")
                .rootCause("Connection pool exhaustion or database saturation.")
                .recommendations(List.of("Check DB health", "Check pool saturation"))
                .iterations(3)
                .usedFallback(false)
                .completedAt(Instant.parse("2026-04-12T09:31:00Z"))
                .build();
    }
}
