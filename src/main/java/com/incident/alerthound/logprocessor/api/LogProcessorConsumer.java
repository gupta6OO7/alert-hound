package com.incident.alerthound.logprocessor.api;

import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.service.LogProcessorService;
import com.incident.alerthound.logprocessor.service.LogTransformationException;
import com.incident.alerthound.logprocessor.service.NonRetryableProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class LogProcessorConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogProcessorConsumer.class);

    private final LogProcessorService logProcessorService;

    public LogProcessorConsumer(LogProcessorService logProcessorService) {
        this.logProcessorService = logProcessorService;
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
            LOGGER.warn(
                    "Skipping invalid log {}: {}",
                    event != null ? event.id() : "unknown",
                    exception.getMessage()
            );
            acknowledgment.acknowledge();
        } catch (NonRetryableProcessingException exception) {
            LOGGER.error(
                    "Skipping log {} because of a non-retryable processing failure: {}",
                    event != null ? event.id() : "unknown",
                    exception.getMessage(),
                    exception
            );
            acknowledgment.acknowledge();
        } catch (RuntimeException exception) {
            LOGGER.error("Transient failure while processing log {}", event != null ? event.id() : "unknown", exception);
            throw exception;
        }
    }
}
