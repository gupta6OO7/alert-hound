package com.incident.alerthound.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alert-hound")
public record AlertHoundProperties(
        KafkaProperties kafka,
        ElasticsearchProperties elasticsearch,
        DetectionProperties detection,
        IncidentProperties incidents,
        AgentProperties agent
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
            TopicDefinition agentTasks,
            TopicDefinition agentResults,
            TopicDefinition incidentsUpdated
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
            String enrichmentConsumerGroupId,
            int activeCacheTtlHours
    ) {
    }

    public record AgentProperties(
            String consumerGroupId,
            int maxIterations,
            int recentLogsLimit,
            int historyLimit,
            int logLookbackMinutes
    ) {
    }
}
