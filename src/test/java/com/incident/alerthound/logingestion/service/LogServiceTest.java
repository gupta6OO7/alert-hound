package com.incident.alerthound.logingestion.service;

import com.incident.alerthound.logingestion.api.LogRequest;
import com.incident.alerthound.logingestion.model.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private LogService logService;

    @Test
    void shouldEnrichAndSendLogEvent() {
        LogRequest request = new LogRequest(
                " payment-service ",
                "error",
                "DB connection timeout",
                "2026-03-31T10:00:00Z",
                null
        );

        String logId = logService.processLog(request);

        ArgumentCaptor<LogEvent> eventCaptor = ArgumentCaptor.forClass(LogEvent.class);
        verify(kafkaProducerService, times(1)).send(eventCaptor.capture());
        LogEvent event = eventCaptor.getValue();

        assertThat(logId).isEqualTo(event.id());
        assertThat(event.service()).isEqualTo("payment-service");
        assertThat(event.level()).isEqualTo("ERROR");
        assertThat(event.timestamp()).isEqualTo("2026-03-31T10:00:00Z");
        assertThat(event.traceId()).isNotBlank();
    }

    @Test
    void shouldDefaultLevelAndTimestampWhenMissing() {
        LogRequest request = new LogRequest(
                "auth-service",
                null,
                "User login failed",
                null,
                "trace-1"
        );

        LogEvent event = logService.enrich(request);

        assertThat(event.level()).isEqualTo("INFO");
        assertThat(event.timestamp()).isNotBlank();
        assertThat(event.traceId()).isEqualTo("trace-1");
    }

    @Test
    void shouldRejectInvalidTimestamp() {
        LogRequest request = new LogRequest(
                "payment-service",
                "ERROR",
                "DB connection timeout",
                "not-a-timestamp",
                null
        );

        assertThatThrownBy(() -> logService.processLog(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timestamp must be a valid ISO-8601 instant");
    }
}
