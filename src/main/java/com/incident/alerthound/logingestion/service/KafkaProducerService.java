package com.incident.alerthound.logingestion.service;

import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.logingestion.model.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String DEFAULT_LOGS_RAW_TOPIC = "logs.raw";

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final String logsRawTopic;

    public KafkaProducerService(KafkaTemplate<String, LogEvent> kafkaTemplate, AlertHoundProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.logsRawTopic = resolveLogsRawTopic(properties);
    }

    private String resolveLogsRawTopic(AlertHoundProperties properties) {
        if (properties == null
                || properties.kafka() == null
                || properties.kafka().topics() == null
                || properties.kafka().topics().logsRaw() == null
                || properties.kafka().topics().logsRaw().name() == null
                || properties.kafka().topics().logsRaw().name().isBlank()) {
            LOGGER.warn("Kafka topic config alert-hound.kafka.topics.logs-raw.name is missing. Falling back to {}", DEFAULT_LOGS_RAW_TOPIC);
            return DEFAULT_LOGS_RAW_TOPIC;
        }

        return properties.kafka().topics().logsRaw().name();
    }

    public void send(LogEvent event) {
        kafkaTemplate.send(logsRawTopic, event.service(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to publish log event with id {}", event.id(), exception);
                        return;
                    }

                    if (result != null) {
                        LOGGER.debug(
                                "Published log event {} to topic {} partition {} offset {}",
                                event.id(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}
