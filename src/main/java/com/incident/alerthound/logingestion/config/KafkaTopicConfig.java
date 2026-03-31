package com.incident.alerthound.logingestion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic logsRawTopic(
            @Value("${alert-hound.kafka.topics.logs-raw}") String topicName,
            @Value("${alert-hound.kafka.topics.logs-raw-partitions}") int partitions,
            @Value("${alert-hound.kafka.topics.logs-raw-replication-factor}") short replicationFactor
    ) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
