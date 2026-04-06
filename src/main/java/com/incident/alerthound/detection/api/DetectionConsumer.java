package com.incident.alerthound.detection.api;

import com.incident.alerthound.detection.service.DetectionService;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DetectionConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectionConsumer.class);

    private final DetectionService detectionService;

    public DetectionConsumer(DetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @KafkaListener(
            topics = "${ALERT_HOUND_KAFKA_TOPIC_LOGS_PROCESSED:logs.processed}",
            groupId = "${ALERT_HOUND_DETECTION_CONSUMER_GROUP_ID:detection-group}",
            containerFactory = "detectionKafkaListenerContainerFactory"
    )
    public void consume(StructuredLog log, Acknowledgment acknowledgment) {
        try {
            detectionService.process(log);
            acknowledgment.acknowledge();
        } catch (RuntimeException exception) {
            LOGGER.error("Detection failed for log {}", log != null ? log.id() : "unknown", exception);
            throw exception;
        }
    }
}
