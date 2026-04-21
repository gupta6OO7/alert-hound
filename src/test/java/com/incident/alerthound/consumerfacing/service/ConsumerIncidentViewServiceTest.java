package com.incident.alerthound.consumerfacing.service;

import com.incident.alerthound.consumerfacing.model.ConsumerIncidentView;
import com.incident.alerthound.incident.model.IncidentStatus;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerIncidentViewServiceTest {

    private final ConsumerIncidentViewService service = new ConsumerIncidentViewService();

    @Test
    void shouldStoreAndReadConsumerIncidentView() {
        IncidentUpdatedEvent event = event(UUID.randomUUID(), "payment", IncidentStatus.INVESTIGATED);

        ConsumerIncidentView view = service.apply(event);

        assertThat(service.get(view.incidentId())).isEqualTo(view);
        assertThat(service.getAll()).containsExactly(view);
    }

    @Test
    void shouldFilterViewsByServiceAndKeepLatestFirst() {
        ConsumerIncidentView older = service.apply(event(UUID.randomUUID(), "payment", IncidentStatus.INVESTIGATED));
        ConsumerIncidentView newer = service.apply(event(UUID.randomUUID(), "payment", IncidentStatus.RESOLVED));
        service.apply(event(UUID.randomUUID(), "orders", IncidentStatus.INVESTIGATED));

        List<ConsumerIncidentView> paymentViews = service.getByService("payment");

        assertThat(paymentViews).containsExactly(newer, older);
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
                .updatedAt(Instant.now())
                .lastDetectedAt(Instant.parse("2026-04-22T00:00:00Z"))
                .build();
    }
}
