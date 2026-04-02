package com.incident.alerthound.logingestion.config;

import com.incident.alerthound.config.AlertHoundProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    @ConditionalOnProperty(prefix = "alert-hound.kafka.admin", name = "auto-create-topics", havingValue = "true")
    public NewTopic logsRawTopic(AlertHoundProperties properties) {
        AlertHoundProperties.TopicDefinition topic = properties.kafka().topics().logsRaw();

        return TopicBuilder.name(topic.name())
                .partitions(topic.partitions())
                .replicas(topic.replicationFactor())
                .build();
    }
}
