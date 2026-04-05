package com.incident.alerthound.logprocessor.service;

import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProcessedLogPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessedLogPublisher.class);
    private static final String DEFAULT_TOPIC = "logs.processed";

    private final KafkaTemplate<String, StructuredLog> kafkaTemplate;
    private final String topicName;

    public ProcessedLogPublisher(KafkaTemplate<String, StructuredLog> kafkaTemplate, AlertHoundProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = resolveTopic(properties);
    }

    public void publish(StructuredLog log) {
        kafkaTemplate.send(topicName, log.service(), log)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to publish processed log {}", log.id(), exception);
                        return;
                    }

                    if (result != null) {
                        LOGGER.debug(
                                "Published processed log {} to topic {} partition {} offset {}",
                                log.id(),
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
                || properties.kafka().topics().logsProcessed() == null
                || properties.kafka().topics().logsProcessed().name() == null
                || properties.kafka().topics().logsProcessed().name().isBlank()) {
            LOGGER.warn("Kafka topic config for processed logs is missing. Falling back to {}", DEFAULT_TOPIC);
            return DEFAULT_TOPIC;
        }

        return properties.kafka().topics().logsProcessed().name();
    }
}
