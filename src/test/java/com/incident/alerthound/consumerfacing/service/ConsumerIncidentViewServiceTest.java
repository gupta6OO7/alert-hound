package com.incident.alerthound.consumerfacing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.consumerfacing.model.ConsumerIncidentView;
import com.incident.alerthound.consumerfacing.model.ConsumerIncidentViewEntity;
import com.incident.alerthound.consumerfacing.repository.ConsumerIncidentViewRepository;
import com.incident.alerthound.incident.model.IncidentStatus;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerIncidentViewServiceTest {

    @Mock
    private ConsumerIncidentViewRepository consumerIncidentViewRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldStoreAndReadConsumerIncidentView() {
        ConsumerIncidentViewService service = new ConsumerIncidentViewService(consumerIncidentViewRepository, objectMapper);
        IncidentUpdatedEvent event = event(UUID.randomUUID(), "payment", IncidentStatus.INVESTIGATED);

        when(consumerIncidentViewRepository.findById(event.incidentId())).thenReturn(Optional.empty());
        when(consumerIncidentViewRepository.save(any(ConsumerIncidentViewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(consumerIncidentViewRepository.findById(event.incidentId()))
                .thenReturn(Optional.of(entity(event)));

        ConsumerIncidentView view = service.apply(event);

        assertThat(service.get(view.incidentId())).usingRecursiveComparison().isEqualTo(view);
        verify(consumerIncidentViewRepository).save(any(ConsumerIncidentViewEntity.class));
    }

    @Test
    void shouldFilterViewsByServiceInProjectionOrder() {
        ConsumerIncidentViewService service = new ConsumerIncidentViewService(consumerIncidentViewRepository, objectMapper);
        ConsumerIncidentViewEntity older = entity(event(UUID.randomUUID(), "payment", IncidentStatus.INVESTIGATED));
        older.setProjectionUpdatedAt(Instant.parse("2026-04-22T01:00:00Z"));
        ConsumerIncidentViewEntity newer = entity(event(UUID.randomUUID(), "payment", IncidentStatus.RESOLVED));
        newer.setProjectionUpdatedAt(Instant.parse("2026-04-22T01:05:00Z"));

        when(consumerIncidentViewRepository.findByServiceIgnoreCaseOrderByProjectionUpdatedAtDesc("payment"))
                .thenReturn(List.of(newer, older));

        List<ConsumerIncidentView> paymentViews = service.getByService("payment");

        assertThat(paymentViews).extracting(ConsumerIncidentView::incidentId)
                .containsExactly(newer.getIncidentId(), older.getIncidentId());
    }

    @Test
    void shouldPersistRecommendationsAsJson() {
        ConsumerIncidentViewService service = new ConsumerIncidentViewService(consumerIncidentViewRepository, objectMapper);
        IncidentUpdatedEvent event = event(UUID.randomUUID(), "payment", IncidentStatus.INVESTIGATED);

        when(consumerIncidentViewRepository.findById(event.incidentId())).thenReturn(Optional.empty());
        when(consumerIncidentViewRepository.save(any(ConsumerIncidentViewEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.apply(event);

        ArgumentCaptor<ConsumerIncidentViewEntity> captor = ArgumentCaptor.forClass(ConsumerIncidentViewEntity.class);
        verify(consumerIncidentViewRepository).save(captor.capture());
        assertThat(captor.getValue().getRecommendationsJson()).contains("restart service");
    }

    private IncidentUpdatedEvent event(UUID incidentId, String serviceName, IncidentStatus status) {
        return IncidentUpdatedEvent.builder()
                .incidentId(incidentId)
                .service(serviceName)
                .status(status)
                .severity("CRITICAL")
                .errorRate(1.0d)
                .summary("summary")
                .rootCause("root cause")
                .recommendations(List.of("restart service"))
                .updatedAt(Instant.parse("2026-04-22T01:10:00Z"))
                .lastDetectedAt(Instant.parse("2026-04-22T01:00:00Z"))
                .build();
    }

    private ConsumerIncidentViewEntity entity(IncidentUpdatedEvent event) {
        ConsumerIncidentViewEntity entity = new ConsumerIncidentViewEntity();
        entity.setIncidentId(event.incidentId());
        entity.setService(event.service());
        entity.setStatus(event.status());
        entity.setSeverity(event.severity());
        entity.setErrorRate(event.errorRate());
        entity.setSummary(event.summary());
        entity.setRootCause(event.rootCause());
        entity.setRecommendationsJson("[\"restart service\"]");
        entity.setUpdatedAt(event.updatedAt());
        entity.setLastDetectedAt(event.lastDetectedAt());
        entity.setProjectionUpdatedAt(Instant.parse("2026-04-22T01:15:00Z"));
        return entity;
    }
}
