package com.incident.alerthound.incident.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.Incident;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncidentUpdateProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentUpdateProducer.class);
    private static final String DEFAULT_TOPIC = "incidents.updated";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final KafkaTemplate<String, IncidentUpdatedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public IncidentUpdateProducer(
            KafkaTemplate<String, IncidentUpdatedEvent> kafkaTemplate,
            ObjectMapper objectMapper,
            AlertHoundProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = resolveTopic(properties);
    }

    public IncidentUpdatedEvent publish(Incident incident) {
        IncidentUpdatedEvent event = toEvent(incident);
        LOGGER.info(
                "Producing incident update incidentId={} service={} status={} topic={}",
                incident.getId(),
                incident.getService(),
                incident.getStatus(),
                topicName
        );
        kafkaTemplate.send(topicName, incident.getService(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to publish incident update for incident {}", incident.getId(), exception);
                        return;
                    }

                    if (result != null) {
                        LOGGER.info(
                                "Published incident update for incident {} to topic {} partition {} offset {}",
                                incident.getId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                })
                .join();
        return event;
    }

    public IncidentUpdatedEvent toEvent(Incident incident) {
        return IncidentUpdatedEvent.builder()
                .incidentId(incident.getId())
                .service(incident.getService())
                .status(incident.getStatus())
                .severity(incident.getSeverity())
                .errorRate(incident.getErrorRate())
                .summary(incident.getSummary())
                .rootCause(incident.getRootCause())
                .recommendations(parseRecommendations(incident.getRecommendationsJson()))
                .updatedAt(incident.getUpdatedAt())
                .lastDetectedAt(incident.getLastDetectedAt())
                .build();
    }

    private List<String> parseRecommendations(String recommendationsJson) {
        if (recommendationsJson == null || recommendationsJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(recommendationsJson, STRING_LIST);
        } catch (Exception exception) {
            LOGGER.warn("Failed to deserialize incident recommendations for update publishing");
            return List.of();
        }
    }

    private String resolveTopic(AlertHoundProperties properties) {
        if (properties == null
                || properties.kafka() == null
                || properties.kafka().topics() == null
                || properties.kafka().topics().incidentsUpdated() == null
                || properties.kafka().topics().incidentsUpdated().name() == null
                || properties.kafka().topics().incidentsUpdated().name().isBlank()) {
            LOGGER.warn("Kafka topic config for incidents.updated is missing. Falling back to {}", DEFAULT_TOPIC);
            return DEFAULT_TOPIC;
        }
        return properties.kafka().topics().incidentsUpdated().name();
    }
}
