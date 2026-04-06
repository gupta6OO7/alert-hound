package com.incident.alerthound.detection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.alerthound.detection.model.IncidentCreatedEvent;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IncidentDebugRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentDebugRepository.class);
    private static final Duration LAST_INCIDENT_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IncidentDebugRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveLastIncident(String service, IncidentCreatedEvent event) {
        try {
            redisTemplate.opsForValue().set(lastIncidentKey(service), objectMapper.writeValueAsString(event), LAST_INCIDENT_TTL);
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Failed to serialize incident event for service {}", service, exception);
        }
    }

    public IncidentCreatedEvent getLastIncident(String service) {
        String payload = redisTemplate.opsForValue().get(lastIncidentKey(service));
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(payload, IncidentCreatedEvent.class);
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Failed to deserialize incident event for service {}", service, exception);
            return null;
        }
    }

    private String lastIncidentKey(String service) {
        return "incident:last:%s".formatted(service);
    }
}
