package com.incident.alerthound.logprocessor.service;

import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.model.InvalidLogEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InvalidLogPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidLogPublisher.class);
    private static final String DEFAULT_TOPIC = "logs.invalid";

    private final KafkaTemplate<String, InvalidLogEvent> kafkaTemplate;
    private final String topicName;

    public InvalidLogPublisher(KafkaTemplate<String, InvalidLogEvent> kafkaTemplate, AlertHoundProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = resolveTopic(properties);
    }

    public void publish(LogEvent event, String reason) {
        InvalidLogEvent invalidLog = InvalidLogEvent.builder()
                .originalEvent(event)
                .reason(reason)
                .failedAt(Instant.now())
                .build();

        kafkaTemplate.send(topicName, event != null ? event.service() : null, invalidLog)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to publish invalid log event {}", event != null ? event.id() : "unknown", exception);
                        return;
                    }

                    if (result != null) {
                        LOGGER.warn(
                                "Published invalid log event {} to topic {} partition {} offset {} because {}",
                                event != null ? event.id() : "unknown",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                reason
                        );
                    }
                })
                .join();
    }

    private String resolveTopic(AlertHoundProperties properties) {
        if (properties == null
                || properties.kafka() == null
                || properties.kafka().topics() == null
                || properties.kafka().topics().logsInvalid() == null
                || properties.kafka().topics().logsInvalid().name() == null
                || properties.kafka().topics().logsInvalid().name().isBlank()) {
            LOGGER.warn("Kafka topic config for invalid logs is missing. Falling back to {}", DEFAULT_TOPIC);
            return DEFAULT_TOPIC;
        }

        return properties.kafka().topics().logsInvalid().name();
    }
}
