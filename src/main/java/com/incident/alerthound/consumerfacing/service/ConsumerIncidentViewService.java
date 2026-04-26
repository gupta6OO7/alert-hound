package com.incident.alerthound.consumerfacing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.consumerfacing.model.ConsumerIncidentView;
import com.incident.alerthound.consumerfacing.model.ConsumerIncidentViewEntity;
import com.incident.alerthound.consumerfacing.repository.ConsumerIncidentViewRepository;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsumerIncidentViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerIncidentViewService.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ConsumerIncidentViewRepository consumerIncidentViewRepository;
    private final ObjectMapper objectMapper;

    public ConsumerIncidentViewService(
            ConsumerIncidentViewRepository consumerIncidentViewRepository,
            ObjectMapper objectMapper
    ) {
        this.consumerIncidentViewRepository = consumerIncidentViewRepository;
        this.objectMapper = objectMapper;
    }

    public ConsumerIncidentView apply(IncidentUpdatedEvent event) {
        ConsumerIncidentViewEntity entity = consumerIncidentViewRepository.findById(event.incidentId())
                .orElseGet(ConsumerIncidentViewEntity::new);
        entity.setIncidentId(event.incidentId());
        entity.setService(event.service());
        entity.setStatus(event.status());
        entity.setSeverity(event.severity());
        entity.setErrorRate(event.errorRate());
        entity.setSummary(event.summary());
        entity.setRootCause(event.rootCause());
        entity.setRecommendationsJson(serializeRecommendations(event.recommendations()));
        entity.setUpdatedAt(event.updatedAt());
        entity.setLastDetectedAt(event.lastDetectedAt());
        entity.setProjectionUpdatedAt(Instant.now());

        ConsumerIncidentView view = toView(consumerIncidentViewRepository.save(entity));
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
        return consumerIncidentViewRepository.findAllByOrderByProjectionUpdatedAtDesc().stream()
                .map(this::toView)
                .toList();
    }

    public ConsumerIncidentView get(UUID incidentId) {
        return consumerIncidentViewRepository.findById(incidentId)
                .map(this::toView)
                .orElse(null);
    }

    public List<ConsumerIncidentView> getByService(String service) {
        return consumerIncidentViewRepository.findByServiceIgnoreCaseOrderByProjectionUpdatedAtDesc(service).stream()
                .map(this::toView)
                .toList();
    }

    private ConsumerIncidentView toView(ConsumerIncidentViewEntity entity) {
        return new ConsumerIncidentView(
                entity.getIncidentId(),
                entity.getService(),
                entity.getStatus(),
                entity.getSeverity(),
                entity.getErrorRate(),
                entity.getSummary(),
                entity.getRootCause(),
                parseRecommendations(entity.getRecommendationsJson()),
                entity.getUpdatedAt(),
                entity.getLastDetectedAt(),
                entity.getProjectionUpdatedAt()
        );
    }

    private String serializeRecommendations(List<String> recommendations) {
        List<String> values = recommendations == null ? List.of() : recommendations.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (values.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize consumer incident view recommendations", exception);
        }
    }

    private List<String> parseRecommendations(String recommendationsJson) {
        if (recommendationsJson == null || recommendationsJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(recommendationsJson, STRING_LIST);
        } catch (Exception exception) {
            LOGGER.warn("Failed to deserialize consumer incident view recommendations");
            return List.of();
        }
    }
}
