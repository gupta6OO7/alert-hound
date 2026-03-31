package com.incident.alerthound.logingestion.service;

import com.incident.alerthound.logingestion.model.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final String logsRawTopic;

    public KafkaProducerService(
            KafkaTemplate<String, LogEvent> kafkaTemplate,
            @Value("${alert-hound.kafka.topics.logs-raw}") String logsRawTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.logsRawTopic = logsRawTopic;
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
