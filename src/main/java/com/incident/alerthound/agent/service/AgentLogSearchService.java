package com.incident.alerthound.agent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentLogSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentLogSearchService.class);

    private final ObjectProvider<ElasticsearchClient> elasticsearchClientProvider;
    private final String indexPattern;

    public AgentLogSearchService(
            ObjectProvider<ElasticsearchClient> elasticsearchClientProvider,
            AlertHoundProperties properties
    ) {
        this.elasticsearchClientProvider = elasticsearchClientProvider;
        this.indexPattern = resolveIndexPattern(properties);
    }

    public List<String> searchRecentLogs(String service, Instant from, Instant to, int limit) {
        ElasticsearchClient client = elasticsearchClientProvider.getIfAvailable();
        if (client == null || !StringUtils.hasText(service) || from == null || to == null || limit <= 0) {
            LOGGER.debug(
                    "Skipping Elasticsearch log search service={} from={} to={} limit={} clientAvailable={}",
                    service,
                    from,
                    to,
                    limit,
                    client != null
            );
            return List.of();
        }

        try {
            LOGGER.debug("Searching Elasticsearch logs service={} from={} to={} limit={} indexPattern={}", service, from, to, limit, indexPattern);
            SearchResponse<StructuredLog> response = client.search(
                    request -> request
                            .index(indexPattern)
                            .size(limit)
                            .sort(sort -> sort.field(field -> field.field("timestamp").order(SortOrder.Desc)))
                            .query(query -> query.bool(bool -> bool
                                    .filter(filter -> filter.term(term -> term.field("service").value(service)))
                                    .filter(filter -> filter.term(term -> term.field("error").value(true))))),
                    StructuredLog.class
            );

            List<String> logs = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                StructuredLog log = hit.source();
                if (log != null && !log.timestamp().isBefore(from) && !log.timestamp().isAfter(to)) {
                    logs.add(formatLog(log));
                }
            });
            LOGGER.debug("Elasticsearch search returned {} logs for service={}", logs.size(), service);
            return logs;
        } catch (IOException exception) {
            LOGGER.warn("Failed to search Elasticsearch for service {}", service, exception);
            return List.of();
        }
    }

    private String resolveIndexPattern(AlertHoundProperties properties) {
        if (properties == null || properties.elasticsearch() == null || !StringUtils.hasText(properties.elasticsearch().logsIndexPattern())) {
            return "logs-*";
        }
        return properties.elasticsearch().logsIndexPattern().trim();
    }

    private String formatLog(StructuredLog log) {
        return log.timestamp() + " [" + log.level() + "] " + log.errorCategory() + " " + log.message();
    }
}
