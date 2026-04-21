package com.incident.alerthound.consumerfacing.service;

import com.incident.alerthound.consumerfacing.model.ConsumerIncidentView;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsumerIncidentViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerIncidentViewService.class);

    private final ConcurrentMap<UUID, ConsumerIncidentView> viewsByIncidentId = new ConcurrentHashMap<>();

    public ConsumerIncidentView apply(IncidentUpdatedEvent event) {
        ConsumerIncidentView view = new ConsumerIncidentView(
                event.incidentId(),
                event.service(),
                event.status(),
                event.severity(),
                event.errorRate(),
                event.summary(),
                event.rootCause(),
                event.recommendations() == null ? List.of() : List.copyOf(event.recommendations()),
                event.updatedAt(),
                event.lastDetectedAt(),
                Instant.now()
        );
        viewsByIncidentId.put(event.incidentId(), view);
        LOGGER.info(
                "Updated consumer incident view incidentId={} service={} status={} recommendations={}",
                event.incidentId(),
                event.service(),
                event.status(),
                view.recommendations().size()
        );
        return view;
    }

    public List<ConsumerIncidentView> getAll() {
        return viewsByIncidentId.values().stream()
                .sorted(Comparator.comparing(ConsumerIncidentView::viewUpdatedAt).reversed())
                .toList();
    }

    public ConsumerIncidentView get(UUID incidentId) {
        return viewsByIncidentId.get(incidentId);
    }

    public List<ConsumerIncidentView> getByService(String service) {
        return viewsByIncidentId.values().stream()
                .filter(view -> view.service().equalsIgnoreCase(service))
                .sorted(Comparator.comparing(ConsumerIncidentView::viewUpdatedAt).reversed())
                .toList();
    }
}
