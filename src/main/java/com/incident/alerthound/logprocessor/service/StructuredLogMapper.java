package com.incident.alerthound.logprocessor.service;

import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StructuredLogMapper {

    public StructuredLog map(LogEvent event) {
        if (event == null) {
            throw new LogTransformationException("log event payload is missing");
        }
        if (!StringUtils.hasText(event.id())) {
            throw new LogTransformationException("log id is missing");
        }
        if (!StringUtils.hasText(event.service())) {
            throw new LogTransformationException("service is missing");
        }
        if (!StringUtils.hasText(event.message())) {
            throw new LogTransformationException("message is missing");
        }

        Instant timestamp = parseTimestamp(event.timestamp());
        String level = normalizeLevel(event.level());
        String message = event.message().trim();

        return StructuredLog.builder()
                .id(event.id().trim())
                .service(event.service().trim())
                .level(level)
                .message(message)
                .timestamp(timestamp)
                .traceId(resolveTraceId(event.traceId()))
                .errorCategory(deriveErrorCategory(level, message))
                .error(isError(level))
                .processedAt(Instant.now())
                .build();
    }

    private Instant parseTimestamp(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            throw new LogTransformationException("timestamp is missing");
        }

        try {
            return Instant.parse(timestamp.trim());
        } catch (DateTimeParseException exception) {
            throw new LogTransformationException("timestamp is not a valid ISO-8601 instant");
        }
    }

    private String normalizeLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return "INFO";
        }
        return level.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveTraceId(String traceId) {
        return StringUtils.hasText(traceId) ? traceId.trim() : "unknown";
    }

    private boolean isError(String level) {
        return "ERROR".equals(level) || "FATAL".equals(level) || "WARN".equals(level);
    }

    private String deriveErrorCategory(String level, String message) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);

        if (!isError(level)) {
            return "NONE";
        }
        if (normalizedMessage.contains("timeout")) {
            return "TIMEOUT";
        }
        if (normalizedMessage.contains("exception")) {
            return "EXCEPTION";
        }
        if (normalizedMessage.contains("connection")) {
            return "CONNECTION";
        }
        return "APPLICATION";
    }
}
