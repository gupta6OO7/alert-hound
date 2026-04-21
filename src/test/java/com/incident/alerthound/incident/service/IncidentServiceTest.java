package com.incident.alerthound.incident.service;

import com.incident.alerthound.detection.model.IncidentCreatedEvent;
import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.model.IncidentStatus;
import com.incident.alerthound.incident.repository.IncidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ActiveIncidentCacheRepository activeIncidentCacheRepository;

    @Mock
    private AgentTaskProducer agentTaskProducer;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void shouldCreateIncidentCacheItAndTriggerAgent() {
        IncidentCreatedEvent event = incidentEvent();
        when(incidentRepository.findFirstByServiceAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq("payment"),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(Optional.empty());
        when(incidentRepository.save(org.mockito.ArgumentMatchers.any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Incident incident = incidentService.handleIncidentCreated(event);

        assertThat(incident.getId()).isEqualTo(UUID.fromString(event.incidentId()));
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);
        assertThat(incident.getService()).isEqualTo("payment");
        verify(activeIncidentCacheRepository).save(org.mockito.ArgumentMatchers.any());
        verify(agentTaskProducer).trigger(incident);
    }

    @Test
    void shouldRefreshExistingLegacyActiveIncidentAndTriggerAgent() {
        IncidentCreatedEvent event = incidentEvent();
        Incident existing = new Incident();
        existing.setId(UUID.randomUUID());
        existing.setService("payment");
        existing.setStatus(IncidentStatus.ACTIVE);
        existing.setSeverity("MEDIUM");
        existing.setErrorRate(0.10d);
        existing.setStartTime(Instant.parse("2026-04-07T09:59:00Z"));
        existing.setLastDetectedAt(Instant.parse("2026-04-07T10:00:00Z"));

        when(incidentRepository.findFirstByServiceAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq("payment"),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(Optional.of(existing));
        when(incidentRepository.save(existing)).thenReturn(existing);

        Incident result = incidentService.handleIncidentCreated(event);

        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getErrorRate()).isEqualTo(0.12d);
        verify(activeIncidentCacheRepository).save(org.mockito.ArgumentMatchers.any());
        verify(agentTaskProducer).trigger(existing);
    }

    @Test
    void shouldRefreshInvestigatedIncidentWithoutRetriggeringAgentWhenFindingsExist() {
        IncidentCreatedEvent event = incidentEvent();
        Incident existing = new Incident();
        existing.setId(UUID.randomUUID());
        existing.setService("payment");
        existing.setStatus(IncidentStatus.INVESTIGATED);
        existing.setSeverity("MEDIUM");
        existing.setErrorRate(0.10d);
        existing.setStartTime(Instant.parse("2026-04-07T09:59:00Z"));
        existing.setLastDetectedAt(Instant.parse("2026-04-07T10:00:00Z"));
        existing.setSummary("DB timeout spike");
        existing.setRootCause("Connection pool saturation");

        when(incidentRepository.findFirstByServiceAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq("payment"),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(Optional.of(existing));
        when(incidentRepository.save(existing)).thenReturn(existing);

        Incident result = incidentService.handleIncidentCreated(event);

        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getStatus()).isEqualTo(IncidentStatus.INVESTIGATED);
        verify(activeIncidentCacheRepository).save(org.mockito.ArgumentMatchers.any());
        verify(agentTaskProducer, never()).trigger(existing);
    }

    @Test
    void shouldResolveIncidentAndRemoveActiveCache() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setId(incidentId);
        incident.setService("payment");
        incident.setStatus(IncidentStatus.ACTIVE);
        incident.setStartTime(Instant.parse("2026-04-07T09:59:00Z"));
        incident.setLastDetectedAt(Instant.parse("2026-04-07T10:00:00Z"));

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        Incident resolved = incidentService.resolveIncident(incidentId);

        assertThat(resolved.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(resolved.getEndTime()).isNotNull();
        verify(activeIncidentCacheRepository).delete("payment");
    }

    @Test
    void shouldReturnActiveIncidentsOrderedFromRepository() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setService("payment");
        incident.setStatus(IncidentStatus.ACTIVE);

        when(incidentRepository.findByStatusInOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(incident));

        List<Incident> incidents = incidentService.getActiveIncidents();

        assertThat(incidents).containsExactly(incident);
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
