package com.incident.alerthound.detection.service;

import com.incident.alerthound.config.AlertHoundProperties;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IncidentDedupRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentDedupRepository.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration dedupTtl;

    public IncidentDedupRepository(StringRedisTemplate redisTemplate, AlertHoundProperties properties) {
        this.redisTemplate = redisTemplate;
        this.dedupTtl = Duration.ofMinutes(properties.detection().dedupTtlMinutes());
    }

    public boolean tryActivate(String service) {
        Boolean created = redisTemplate.opsForValue()
                .setIfAbsent(activeIncidentKey(service), "true", dedupTtl);
        boolean activated = Boolean.TRUE.equals(created);
        LOGGER.debug("Incident dedup service={} activated={} ttl={}", service, activated, dedupTtl);
        return activated;
    }

    public boolean isActive(String service) {
        Boolean exists = redisTemplate.hasKey(activeIncidentKey(service));
        return Boolean.TRUE.equals(exists);
    }

    public Instant expiresAt(String service) {
        Long ttlSeconds = redisTemplate.getExpire(activeIncidentKey(service));
        if (ttlSeconds == null || ttlSeconds < 0) {
            return null;
        }
        return Instant.now().plusSeconds(ttlSeconds);
    }

    private String activeIncidentKey(String service) {
        return "incident:dedup:%s".formatted(service);
    }
}
