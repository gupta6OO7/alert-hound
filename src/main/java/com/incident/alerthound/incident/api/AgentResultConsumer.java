package com.incident.alerthound.incident.api;

import com.incident.alerthound.agent.model.AgentResultEvent;
import com.incident.alerthound.incident.service.IncidentEnrichmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class AgentResultConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentResultConsumer.class);

    private final IncidentEnrichmentService incidentEnrichmentService;

    public AgentResultConsumer(IncidentEnrichmentService incidentEnrichmentService) {
        this.incidentEnrichmentService = incidentEnrichmentService;
    }

    @KafkaListener(
            topics = "${ALERT_HOUND_KAFKA_TOPIC_AGENT_RESULTS:agent.results}",
            groupId = "${ALERT_HOUND_INCIDENT_ENRICHMENT_CONSUMER_GROUP_ID:incident-enrichment-group}",
            containerFactory = "agentResultIncidentKafkaListenerContainerFactory"
    )
    public void consume(AgentResultEvent event, Acknowledgment acknowledgment) {
        try {
            LOGGER.info(
                    "Consumed agent result incidentId={} service={} usedFallback={} iterations={}",
                    event != null ? event.incidentId() : "unknown",
                    event != null ? event.service() : "unknown",
                    event != null && event.usedFallback(),
                    event != null ? event.iterations() : -1
            );
            incidentEnrichmentService.process(event);
            acknowledgment.acknowledge();
            LOGGER.debug("Acknowledged agent result incidentId={}", event != null ? event.incidentId() : "unknown");
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to enrich incident from agent result {}", event != null ? event.incidentId() : "unknown", exception);
            throw exception;
        }
    }
}
