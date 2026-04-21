package com.incident.alerthound.agent.api;

import com.incident.alerthound.agent.service.AgentService;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.AgentTaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class AgentTaskConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentTaskConsumer.class);

    private final AgentService agentService;
    private final String topicName;
    private final String groupId;

    public AgentTaskConsumer(AgentService agentService, AlertHoundProperties properties) {
        this.agentService = agentService;
        this.topicName = resolveTopic(properties);
        this.groupId = resolveGroupId(properties);
    }

    @KafkaListener(
            topics = "#{__listener.topicName}",
            groupId = "#{__listener.groupId}",
            containerFactory = "agentKafkaListenerContainerFactory"
    )
    public void consume(AgentTaskEvent event, Acknowledgment acknowledgment) {
        try {
            LOGGER.info(
                    "Consumed agent task incidentId={} service={} severity={}",
                    event != null ? event.incidentId() : "unknown",
                    event != null ? event.service() : "unknown",
                    event != null ? event.severity() : "unknown"
            );
            agentService.handle(event);
            acknowledgment.acknowledge();
            LOGGER.debug("Acknowledged agent task incidentId={}", event != null ? event.incidentId() : "unknown");
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to process agent task {}", event != null ? event.incidentId() : "unknown", exception);
            throw exception;
        }
    }

    public String getTopicName() {
        return topicName;
    }

    public String getGroupId() {
        return groupId;
    }

    private String resolveTopic(AlertHoundProperties properties) {
        if (properties == null
                || properties.kafka() == null
                || properties.kafka().topics() == null
                || properties.kafka().topics().agentTasks() == null
                || properties.kafka().topics().agentTasks().name() == null
                || properties.kafka().topics().agentTasks().name().isBlank()) {
            return "agent.tasks";
        }
        return properties.kafka().topics().agentTasks().name();
    }

    private String resolveGroupId(AlertHoundProperties properties) {
        if (properties == null || properties.agent() == null || properties.agent().consumerGroupId() == null
                || properties.agent().consumerGroupId().isBlank()) {
            return "agent-group";
        }
        return properties.agent().consumerGroupId();
    }
}
