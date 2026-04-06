package com.incident.alerthound.logprocessor.api;

import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.service.LogProcessorService;
import com.incident.alerthound.logprocessor.service.LogTransformationException;
import com.incident.alerthound.logprocessor.service.NonRetryableProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogProcessorConsumerTest {

    @Mock
    private LogProcessorService logProcessorService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldSkipInvalidLogsAndAcknowledge() {
        LogEvent event = LogEvent.builder()
                .id("bad-log")
                .service("payment")
                .message("oops")
                .timestamp("bad")
                .build();

        LogProcessorConsumer consumer = new LogProcessorConsumer(logProcessorService);
        when(logProcessorService.process(event)).thenThrow(new LogTransformationException("timestamp is not a valid ISO-8601 instant"));

        consumer.consume(event, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowTransientFailuresForRetry() {
        LogEvent event = LogEvent.builder()
                .id("log-1")
                .service("payment")
                .message("timeout")
                .timestamp("2026-04-06T09:30:00Z")
                .build();

        LogProcessorConsumer consumer = new LogProcessorConsumer(logProcessorService);
        when(logProcessorService.process(event)).thenThrow(new IllegalStateException("Elasticsearch unavailable"));

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Elasticsearch unavailable");
    }

    @Test
    void shouldAcknowledgeNonRetryableFailures() {
        LogEvent event = LogEvent.builder()
                .id("log-2")
                .service("payment")
                .message("timeout")
                .timestamp("2026-04-06T09:30:00Z")
                .build();

        LogProcessorConsumer consumer = new LogProcessorConsumer(logProcessorService);
        when(logProcessorService.process(event)).thenThrow(
                new NonRetryableProcessingException("Elasticsearch authentication failed", new RuntimeException("401"))
        );

        consumer.consume(event, acknowledgment);

        verify(acknowledgment).acknowledge();
    }
}
