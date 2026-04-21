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
    private static final int DEFAULT_ACTIVE_CACHE_TTL_HOURS = 24;

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
        int configuredTtlHours = properties != null && properties.incidents() != null && properties.incidents().activeCacheTtlHours() > 0
                ? properties.incidents().activeCacheTtlHours()
                : DEFAULT_ACTIVE_CACHE_TTL_HOURS;
        this.ttl = Duration.ofHours(configuredTtlHours);
    }

    public void save(ActiveIncidentState state) {
        try {
            redisTemplate.opsForValue().set(cacheKey(state.service()), objectMapper.writeValueAsString(state), ttl);
            LOGGER.debug(
                    "Saved active incident cache service={} incidentId={} status={} ttl={}",
                    state.service(),
                    state.incidentId(),
                    state.status(),
                    ttl
            );
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Failed to serialize active incident cache for service {}", state.service(), exception);
        }
    }

    public ActiveIncidentState get(String service) {
        String payload = redisTemplate.opsForValue().get(cacheKey(service));
        if (payload == null || payload.isBlank()) {
            LOGGER.debug("Active incident cache miss for service={}", service);
            return null;
        }

        try {
            ActiveIncidentState state = objectMapper.readValue(payload, ActiveIncidentState.class);
            LOGGER.debug("Loaded active incident cache service={} incidentId={} status={}", service, state.incidentId(), state.status());
            return state;
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
        LOGGER.debug("Deleted active incident cache service={}", service);
    }

    private String cacheKey(String service) {
        return "incident:active:%s".formatted(service);
    }
}
