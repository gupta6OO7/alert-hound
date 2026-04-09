package com.incident.alerthound.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alert-hound")
public record AlertHoundProperties(
        KafkaProperties kafka,
        ElasticsearchProperties elasticsearch,
        DetectionProperties detection,
        IncidentProperties incidents
) {

    public record KafkaProperties(
            String clusterId,
            String restEndpoint,
            AdminProperties admin,
            TopicProperties topics
    ) {
    }

    public record AdminProperties(
            boolean autoCreateTopics
    ) {
    }

    public record TopicProperties(
            TopicDefinition logsRaw,
            TopicDefinition logsProcessed,
            TopicDefinition incidentsCreated,
            TopicDefinition agentTasks
    ) {
    }

    public record TopicDefinition(
            String name,
            int partitions,
            short replicationFactor
    ) {
    }

    public record ElasticsearchProperties(
            String endpoint,
            String apiKey,
            String logsIndexPattern
    ) {
    }

    public record DetectionProperties(
            String consumerGroupId,
            int windowSizeMinutes,
            int counterTtlMinutes,
            int dedupTtlMinutes,
            long minimumTotalLogs,
            double errorRateThreshold
    ) {
    }

    public record IncidentProperties(
            String consumerGroupId,
            int activeCacheTtlHours
    ) {
    }
}
