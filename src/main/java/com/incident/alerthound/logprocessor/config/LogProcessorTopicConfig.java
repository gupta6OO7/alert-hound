package com.incident.alerthound.logprocessor.config;

import com.incident.alerthound.config.AlertHoundProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class LogProcessorTopicConfig {

    @Bean
    @ConditionalOnProperty(prefix = "alert-hound.kafka.admin", name = "auto-create-topics", havingValue = "true")
    public NewTopic logsProcessedTopic(AlertHoundProperties properties) {
        return buildTopic(properties != null && properties.kafka() != null && properties.kafka().topics() != null
                ? properties.kafka().topics().logsProcessed()
                : null, "logs.processed");
    }

    @Bean
    @ConditionalOnProperty(prefix = "alert-hound.kafka.admin", name = "auto-create-topics", havingValue = "true")
    public NewTopic logsInvalidTopic(AlertHoundProperties properties) {
        return buildTopic(properties != null && properties.kafka() != null && properties.kafka().topics() != null
                ? properties.kafka().topics().logsInvalid()
                : null, "logs.invalid");
    }

    @Bean
    @ConditionalOnProperty(prefix = "alert-hound.kafka.admin", name = "auto-create-topics", havingValue = "true")
    public NewTopic logsFailedTopic(AlertHoundProperties properties) {
        return buildTopic(properties != null && properties.kafka() != null && properties.kafka().topics() != null
                ? properties.kafka().topics().logsFailed()
                : null, "logs.failed");
    }

    private NewTopic buildTopic(AlertHoundProperties.TopicDefinition topic, String defaultName) {
        int partitions = topic != null && topic.partitions() > 0 ? topic.partitions() : 3;
        short replicationFactor = topic != null && topic.replicationFactor() > 0 ? topic.replicationFactor() : 1;
        String topicName = topic != null && topic.name() != null && !topic.name().isBlank() ? topic.name() : defaultName;

        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
