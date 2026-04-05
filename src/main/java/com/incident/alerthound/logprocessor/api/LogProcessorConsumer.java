package com.incident.alerthound.logprocessor.api;

import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.service.InvalidLogPublisher;
import com.incident.alerthound.logprocessor.service.LogProcessorService;
import com.incident.alerthound.logprocessor.service.LogTransformationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class LogProcessorConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogProcessorConsumer.class);

    private final LogProcessorService logProcessorService;
    private final InvalidLogPublisher invalidLogPublisher;

    public LogProcessorConsumer(LogProcessorService logProcessorService, InvalidLogPublisher invalidLogPublisher) {
        this.logProcessorService = logProcessorService;
        this.invalidLogPublisher = invalidLogPublisher;
    }

    @KafkaListener(
            topics = "${ALERT_HOUND_KAFKA_TOPIC_LOGS_RAW:logs.raw}",
            groupId = "${ALERT_HOUND_KAFKA_CONSUMER_GROUP_ID:log-processor-group}",
            containerFactory = "logProcessorKafkaListenerContainerFactory"
    )
    public void consume(LogEvent event, Acknowledgment acknowledgment) {
        try {
            logProcessorService.process(event);
            acknowledgment.acknowledge();
        } catch (LogTransformationException exception) {
            invalidLogPublisher.publish(event, exception.getMessage());
            acknowledgment.acknowledge();
        } catch (RuntimeException exception) {
            LOGGER.error("Transient failure while processing log {}", event != null ? event.id() : "unknown", exception);
            throw exception;
        }
    }
}
