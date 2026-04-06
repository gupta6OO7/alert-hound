package com.incident.alerthound.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnExpression("'${alert-hound.elasticsearch.endpoint:}' != ''")
public class ElasticsearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient(AlertHoundProperties properties) {
        AlertHoundProperties.ElasticsearchProperties elasticsearch = properties.elasticsearch();

        RestClientBuilder builder = RestClient.builder(HttpHost.create(elasticsearch.endpoint()));

        if (StringUtils.hasText(elasticsearch.apiKey())) {
            builder.setDefaultHeaders(new Header[]{
                    new BasicHeader("Authorization", "ApiKey " + encodeApiKey(elasticsearch.apiKey()))
            });
        }

        return builder.build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient, ObjectMapper objectMapper) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    private String encodeApiKey(String apiKey) {
        String trimmedApiKey = apiKey.trim();
        if (trimmedApiKey.contains(":")) {
            return Base64.getEncoder().encodeToString(trimmedApiKey.getBytes(StandardCharsets.UTF_8));
        }
        return trimmedApiKey;
    }
}
