package com.incident.alerthound.agent.api;

import com.incident.alerthound.agent.service.AgentService;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.AgentTaskEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class AgentTaskConsumer {

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
        agentService.handle(event);
        acknowledgment.acknowledge();
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
