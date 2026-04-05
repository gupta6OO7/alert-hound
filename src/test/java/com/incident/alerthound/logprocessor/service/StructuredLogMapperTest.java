package com.incident.alerthound.logprocessor.service;

import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredLogMapperTest {

    private final StructuredLogMapper mapper = new StructuredLogMapper();

    @Test
    void shouldMapAndEnrichErrorLog() {
        LogEvent event = LogEvent.builder()
                .id("log-1")
                .service("payment-service")
                .level("error")
                .message("Database connection timeout Exception")
                .timestamp("2026-04-06T09:30:00Z")
                .traceId("trace-1")
                .build();

        StructuredLog structuredLog = mapper.map(event);

        assertThat(structuredLog.id()).isEqualTo("log-1");
        assertThat(structuredLog.level()).isEqualTo("ERROR");
        assertThat(structuredLog.error()).isTrue();
        assertThat(structuredLog.errorCategory()).isEqualTo("TIMEOUT");
        assertThat(structuredLog.processedAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidTimestamp() {
        LogEvent event = LogEvent.builder()
                .id("log-1")
                .service("payment-service")
                .level("INFO")
                .message("hello")
                .timestamp("bad")
                .traceId("trace-1")
                .build();

        assertThatThrownBy(() -> mapper.map(event))
                .isInstanceOf(LogTransformationException.class)
                .hasMessage("timestamp is not a valid ISO-8601 instant");
    }
}
