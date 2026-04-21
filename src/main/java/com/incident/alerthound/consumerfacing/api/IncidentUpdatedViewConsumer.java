package com.incident.alerthound.consumerfacing.api;

import com.incident.alerthound.consumerfacing.service.ConsumerIncidentViewService;
import com.incident.alerthound.incident.model.IncidentUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class IncidentUpdatedViewConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentUpdatedViewConsumer.class);

    private final ConsumerIncidentViewService consumerIncidentViewService;

    public IncidentUpdatedViewConsumer(ConsumerIncidentViewService consumerIncidentViewService) {
        this.consumerIncidentViewService = consumerIncidentViewService;
    }

    @KafkaListener(
            topics = "${ALERT_HOUND_KAFKA_TOPIC_INCIDENTS_UPDATED:incidents.updated}",
            groupId = "${ALERT_HOUND_CONSUMER_FACING_GROUP_ID:incident-view-group}",
            containerFactory = "incidentUpdatedViewKafkaListenerContainerFactory"
    )
    public void consume(IncidentUpdatedEvent event, Acknowledgment acknowledgment) {
        try {
            LOGGER.info(
                    "Consumed incident update for view incidentId={} service={} status={}",
                    event != null ? event.incidentId() : "unknown",
                    event != null ? event.service() : "unknown",
                    event != null ? event.status() : null
            );
            consumerIncidentViewService.apply(event);
            acknowledgment.acknowledge();
            LOGGER.debug("Acknowledged consumer-facing update incidentId={}", event != null ? event.incidentId() : "unknown");
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to update consumer incident view for incident {}", event != null ? event.incidentId() : "unknown", exception);
            throw exception;
        }
    }
}
