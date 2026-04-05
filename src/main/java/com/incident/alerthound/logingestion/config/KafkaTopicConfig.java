package com.incident.alerthound.logingestion.config;

import com.incident.alerthound.config.AlertHoundProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    private static final String DEFAULT_LOGS_RAW_TOPIC = "logs.raw";
    private static final int DEFAULT_LOGS_RAW_PARTITIONS = 3;
    private static final short DEFAULT_LOGS_RAW_REPLICATION_FACTOR = 1;

    @Bean
    @ConditionalOnProperty(prefix = "alert-hound.kafka.admin", name = "auto-create-topics", havingValue = "true")
    public NewTopic logsRawTopic(AlertHoundProperties properties) {
        String topicName = DEFAULT_LOGS_RAW_TOPIC;
        int partitions = DEFAULT_LOGS_RAW_PARTITIONS;
        short replicationFactor = DEFAULT_LOGS_RAW_REPLICATION_FACTOR;

        if (properties != null
                && properties.kafka() != null
                && properties.kafka().topics() != null
                && properties.kafka().topics().logsRaw() != null) {
            AlertHoundProperties.TopicDefinition topic = properties.kafka().topics().logsRaw();
            if (topic.name() != null && !topic.name().isBlank()) {
                topicName = topic.name();
            }
            if (topic.partitions() > 0) {
                partitions = topic.partitions();
            }
            if (topic.replicationFactor() > 0) {
                replicationFactor = topic.replicationFactor();
            }
        }

        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
