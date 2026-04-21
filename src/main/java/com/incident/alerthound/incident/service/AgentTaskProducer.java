package com.incident.alerthound.incident.service;

import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.AgentTaskEvent;
import com.incident.alerthound.incident.model.Incident;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AgentTaskProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentTaskProducer.class);
    private static final String DEFAULT_TOPIC = "agent.tasks";

    private final KafkaTemplate<String, AgentTaskEvent> kafkaTemplate;
    private final String topicName;

    public AgentTaskProducer(KafkaTemplate<String, AgentTaskEvent> kafkaTemplate, AlertHoundProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = resolveTopic(properties);
    }

    public void trigger(Incident incident) {
        AgentTaskEvent event = AgentTaskEvent.builder()
                .incidentId(incident.getId())
                .service(incident.getService())
                .severity(incident.getSeverity())
                .errorRate(incident.getErrorRate())
                .startTime(incident.getStartTime())
                .triggeredAt(Instant.now())
                .build();

        LOGGER.info(
                "Producing agent task incidentId={} service={} severity={} topic={}",
                incident.getId(),
                incident.getService(),
                incident.getSeverity(),
                topicName
        );
        kafkaTemplate.send(topicName, incident.getService(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to publish agent task for incident {}", incident.getId(), exception);
                        return;
                    }

                    if (result != null) {
                        LOGGER.info(
                                "Published agent task for incident {} to topic {} partition {} offset {}",
                                incident.getId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                })
                .join();
    }

    private String resolveTopic(AlertHoundProperties properties) {
        if (properties == null
                || properties.kafka() == null
                || properties.kafka().topics() == null
                || properties.kafka().topics().agentTasks() == null
                || properties.kafka().topics().agentTasks().name() == null
                || properties.kafka().topics().agentTasks().name().isBlank()) {
            LOGGER.warn("Kafka topic config for agent.tasks is missing. Falling back to {}", DEFAULT_TOPIC);
            return DEFAULT_TOPIC;
        }

        return properties.kafka().topics().agentTasks().name();
    }
}
