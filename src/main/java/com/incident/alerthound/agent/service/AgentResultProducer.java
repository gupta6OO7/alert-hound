package com.incident.alerthound.agent.service;

import com.incident.alerthound.agent.model.AgentResultEvent;
import com.incident.alerthound.config.AlertHoundProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AgentResultProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentResultProducer.class);
    private static final String DEFAULT_TOPIC = "agent.results";

    private final KafkaTemplate<String, AgentResultEvent> kafkaTemplate;
    private final String topicName;

    public AgentResultProducer(KafkaTemplate<String, AgentResultEvent> kafkaTemplate, AlertHoundProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = resolveTopic(properties);
    }

    public void publish(AgentResultEvent event) {
        kafkaTemplate.send(topicName, event.service(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to publish agent result for incident {}", event.incidentId(), exception);
                        return;
                    }

                    if (result != null) {
                        LOGGER.info(
                                "Published agent result for incident {} to topic {} partition {} offset {}",
                                event.incidentId(),
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
                || properties.kafka().topics().agentResults() == null
                || properties.kafka().topics().agentResults().name() == null
                || properties.kafka().topics().agentResults().name().isBlank()) {
            LOGGER.warn("Kafka topic config for agent.results is missing. Falling back to {}", DEFAULT_TOPIC);
            return DEFAULT_TOPIC;
        }

        return properties.kafka().topics().agentResults().name();
    }
}
