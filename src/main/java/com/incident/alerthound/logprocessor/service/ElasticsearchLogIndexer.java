package com.incident.alerthound.logprocessor.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ElasticsearchLogIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchLogIndexer.class);
    private static final DateTimeFormatter INDEX_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String DEFAULT_INDEX_PREFIX = "logs-";

    private final ElasticsearchClient elasticsearchClient;
    private final String indexPrefix;
    private final Map<String, Boolean> knownIndices = new ConcurrentHashMap<>();

    public ElasticsearchLogIndexer(ElasticsearchClient elasticsearchClient, AlertHoundProperties properties) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexPrefix = resolveIndexPrefix(properties);
    }

    @PostConstruct
    void logConfiguration() {
        LOGGER.info("Elasticsearch log index prefix resolved to {}", indexPrefix);
    }

    public void index(StructuredLog log) {
        String indexName = buildIndexName(log);
        ensureIndex(indexName);

        try {
            elasticsearchClient.index(request -> request
                    .index(indexName)
                    .id(log.id())
                    .document(log));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to index log " + log.id() + " into Elasticsearch", exception);
        }
    }

    String buildIndexName(StructuredLog log) {
        LocalDate date = LocalDate.ofInstant(log.timestamp(), ZoneOffset.UTC);
        return indexPrefix + INDEX_DATE_FORMAT.format(date);
    }

    private void ensureIndex(String indexName) {
        if (knownIndices.putIfAbsent(indexName, Boolean.TRUE) != null) {
            return;
        }

        try {
            boolean exists = elasticsearchClient.indices().exists(request -> request.index(indexName)).value();
            if (exists) {
                return;
            }

            CreateIndexRequest request = new CreateIndexRequest.Builder()
                    .index(indexName)
                    .mappings(mapping -> mapping.properties("timestamp", Property.of(p -> p.date(d -> d)))
                            .properties("service", Property.of(p -> p.keyword(k -> k)))
                            .properties("level", Property.of(p -> p.keyword(k -> k)))
                            .properties("message", Property.of(p -> p.text(t -> t)))
                            .properties("traceId", Property.of(p -> p.keyword(k -> k)))
                            .properties("errorCategory", Property.of(p -> p.keyword(k -> k)))
                            .properties("error", Property.of(p -> p.boolean_(b -> b)))
                            .properties("processedAt", Property.of(p -> p.date(d -> d))))
                    .build();

            elasticsearchClient.indices().create(request);
            LOGGER.info("Created Elasticsearch index {}", indexName);
        } catch (IOException exception) {
            knownIndices.remove(indexName);
            throw new IllegalStateException("Failed to ensure Elasticsearch index " + indexName, exception);
        }
    }

    private String resolveIndexPrefix(AlertHoundProperties properties) {
        if (properties == null || properties.elasticsearch() == null || !StringUtils.hasText(properties.elasticsearch().logsIndexPattern())) {
            return DEFAULT_INDEX_PREFIX;
        }

        String pattern = properties.elasticsearch().logsIndexPattern().trim();
        if (pattern.endsWith("*")) {
            return pattern.substring(0, pattern.length() - 1);
        }

        return pattern.endsWith("-") ? pattern : pattern + "-";
    }
}
