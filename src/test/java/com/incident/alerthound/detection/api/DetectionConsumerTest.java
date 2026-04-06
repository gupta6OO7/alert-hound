package com.incident.alerthound.detection.api;

import com.incident.alerthound.detection.service.DetectionService;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DetectionConsumerTest {

    @Mock
    private DetectionService detectionService;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldAcknowledgeAfterSuccessfulDetection() {
        StructuredLog log = log();
        DetectionConsumer consumer = new DetectionConsumer(detectionService);

        consumer.consume(log, acknowledgment);

        verify(detectionService).process(log);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowFailuresForRetry() {
        StructuredLog log = log();
        DetectionConsumer consumer = new DetectionConsumer(detectionService);
        doThrow(new IllegalStateException("Redis unavailable")).when(detectionService).process(log);

        assertThatThrownBy(() -> consumer.consume(log, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Redis unavailable");
    }

    private StructuredLog log() {
        return StructuredLog.builder()
                .id("log-1")
                .service("payment")
                .level("ERROR")
                .message("timeout")
                .timestamp(Instant.parse("2026-04-06T10:01:10Z"))
                .traceId("trace-1")
                .errorCategory("TIMEOUT")
                .error(true)
                .processedAt(Instant.parse("2026-04-06T10:01:11Z"))
                .build();
    }
}
