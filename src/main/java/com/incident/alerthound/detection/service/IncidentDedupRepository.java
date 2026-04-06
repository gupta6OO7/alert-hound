package com.incident.alerthound.detection.service;

import com.incident.alerthound.config.AlertHoundProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IncidentDedupRepository {

    private final StringRedisTemplate redisTemplate;
    private final Duration dedupTtl;

    public IncidentDedupRepository(StringRedisTemplate redisTemplate, AlertHoundProperties properties) {
        this.redisTemplate = redisTemplate;
        this.dedupTtl = Duration.ofMinutes(properties.detection().dedupTtlMinutes());
    }

    public boolean tryActivate(String service) {
        Boolean created = redisTemplate.opsForValue()
                .setIfAbsent(activeIncidentKey(service), "true", dedupTtl);
        return Boolean.TRUE.equals(created);
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
        return "incident:active:%s".formatted(service);
    }
}
