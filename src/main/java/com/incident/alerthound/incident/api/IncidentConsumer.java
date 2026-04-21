package com.incident.alerthound.incident.api;

import com.incident.alerthound.detection.model.IncidentCreatedEvent;
import com.incident.alerthound.incident.service.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class IncidentConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentConsumer.class);

    private final IncidentService incidentService;

    public IncidentConsumer(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @KafkaListener(
            topics = "${ALERT_HOUND_KAFKA_TOPIC_INCIDENTS_CREATED:incidents.created}",
            groupId = "${ALERT_HOUND_INCIDENTS_CONSUMER_GROUP_ID:incident-group}",
            containerFactory = "incidentKafkaListenerContainerFactory"
    )
    public void consume(IncidentCreatedEvent event, Acknowledgment acknowledgment) {
        try {
            LOGGER.info(
                    "Consumed incident event incidentId={} service={} severity={}",
                    event != null ? event.incidentId() : "unknown",
                    event != null ? event.service() : "unknown",
                    event != null ? event.severity() : "unknown"
            );
            incidentService.handleIncidentCreated(event);
            acknowledgment.acknowledge();
            LOGGER.debug("Acknowledged incident event incidentId={}", event != null ? event.incidentId() : "unknown");
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to process incident event {}", event != null ? event.incidentId() : "unknown", exception);
            throw exception;
        }
    }
}
