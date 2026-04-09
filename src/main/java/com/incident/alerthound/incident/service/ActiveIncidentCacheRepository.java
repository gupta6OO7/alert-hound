package com.incident.alerthound.incident.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.config.AlertHoundProperties;
import com.incident.alerthound.incident.model.ActiveIncidentState;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveIncidentCacheRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveIncidentCacheRepository.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public ActiveIncidentCacheRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AlertHoundProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofHours(properties.incidents().activeCacheTtlHours());
    }

    public void save(ActiveIncidentState state) {
        try {
            redisTemplate.opsForValue().set(cacheKey(state.service()), objectMapper.writeValueAsString(state), ttl);
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Failed to serialize active incident cache for service {}", state.service(), exception);
        }
    }

    public ActiveIncidentState get(String service) {
        String payload = redisTemplate.opsForValue().get(cacheKey(service));
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(payload, ActiveIncidentState.class);
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Failed to deserialize active incident cache for service {}", service, exception);
            return null;
        }
    }

    public boolean exists(String service) {
        Boolean exists = redisTemplate.hasKey(cacheKey(service));
        return Boolean.TRUE.equals(exists);
    }

    public void delete(String service) {
        redisTemplate.delete(cacheKey(service));
    }

    private String cacheKey(String service) {
        return "incident:active:%s".formatted(service);
    }
}
