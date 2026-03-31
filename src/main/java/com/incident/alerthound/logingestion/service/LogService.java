package com.incident.alerthound.logingestion.service;

import com.incident.alerthound.logingestion.api.LogRequest;
import com.incident.alerthound.logingestion.model.LogEvent;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LogService {

    private static final String DEFAULT_LEVEL = "INFO";

    private final KafkaProducerService kafkaProducerService;

    public LogService(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    public String processLog(LogRequest request) {
        LogEvent event = enrich(request);
        kafkaProducerService.send(event);
        return event.id();
    }

    LogEvent enrich(LogRequest request) {
        return LogEvent.builder()
                .id(UUID.randomUUID().toString())
                .service(normalizeService(request.service()))
                .level(normalizeLevel(request.level()))
                .message(request.message().trim())
                .timestamp(resolveTimestamp(request.timestamp()))
                .traceId(resolveTraceId(request.traceId()))
                .build();
    }

    private String normalizeService(String service) {
        String normalized = service.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("service must not be blank");
        }
        return normalized;
    }

    private String normalizeLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return DEFAULT_LEVEL;
        }
        return level.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveTimestamp(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return Instant.now().toString();
        }

        try {
            return Instant.parse(timestamp.trim()).toString();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("timestamp must be a valid ISO-8601 instant");
        }
    }

    private String resolveTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return UUID.randomUUID().toString();
        }
        return traceId.trim();
    }
}
