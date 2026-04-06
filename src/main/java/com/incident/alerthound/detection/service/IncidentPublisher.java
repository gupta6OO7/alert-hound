package com.incident.alerthound.detection.service;

import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.detection.model.IncidentCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncidentPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentPublisher.class);
    private static final String DEFAULT_TOPIC = "incidents.created";

    private final KafkaTemplate<String, IncidentCreatedEvent> kafkaTemplate;
    private final IncidentDebugRepository incidentDebugRepository;
    private final String topicName;

    public IncidentPublisher(
            KafkaTemplate<String, IncidentCreatedEvent> kafkaTemplate,
            IncidentDebugRepository incidentDebugRepository,
            AlertHoundProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.incidentDebugRepository = incidentDebugRepository;
        this.topicName = resolveTopic(properties);
    }

    public void publish(String service, DetectionDecision decision) {
        IncidentCreatedEvent event = IncidentCreatedEvent.builder()
                .incidentId(UUID.randomUUID().toString())
                .service(service)
                .windowStart(decision.windowStart())
                .windowEnd(decision.windowEnd())
                .triggeredAt(Instant.now())
                .totalLogs(decision.totalLogs())
                .errorLogs(decision.errorLogs())
                .errorRate(decision.errorRate())
                .severity(resolveSeverity(decision.errorRate()))
                .reason(decision.reason())
                .build();

        kafkaTemplate.send(topicName, service, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to publish incident for service {}", service, exception);
                        return;
                    }

                    if (result != null) {
                        LOGGER.info(
                                "Published incident {} for service {} to topic {} partition {} offset {}",
                                event.incidentId(),
                                service,
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                })
                .join();

        incidentDebugRepository.saveLastIncident(service, event);
    }

    private String resolveTopic(AlertHoundProperties properties) {
        if (properties == null
                || properties.kafka() == null
                || properties.kafka().topics() == null
                || properties.kafka().topics().incidentsCreated() == null
                || properties.kafka().topics().incidentsCreated().name() == null
                || properties.kafka().topics().incidentsCreated().name().isBlank()) {
            LOGGER.warn("Kafka topic config for incidents.created is missing. Falling back to {}", DEFAULT_TOPIC);
            return DEFAULT_TOPIC;
        }

        return properties.kafka().topics().incidentsCreated().name();
    }

    private String resolveSeverity(double errorRate) {
        if (errorRate >= 0.20d) {
            return "CRITICAL";
        }
        if (errorRate >= 0.10d) {
            return "HIGH";
        }
        return "MEDIUM";
    }
}
