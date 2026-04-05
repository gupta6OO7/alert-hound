package com.incident.alerthound.logprocessor.service;

import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogProcessorServiceTest {

    @Mock
    private StructuredLogMapper structuredLogMapper;

    @Mock
    private ElasticsearchLogIndexer elasticsearchLogIndexer;

    @Mock
    private ProcessedLogPublisher processedLogPublisher;

    @InjectMocks
    private LogProcessorService logProcessorService;

    @Test
    void shouldIndexAndPublishProcessedLog() {
        LogEvent event = LogEvent.builder()
                .id("log-1")
                .service("payment-service")
                .level("ERROR")
                .message("timeout")
                .timestamp("2026-04-06T09:30:00Z")
                .traceId("trace-1")
                .build();

        StructuredLog structuredLog = StructuredLog.builder()
                .id("log-1")
                .service("payment-service")
                .level("ERROR")
                .message("timeout")
                .timestamp(Instant.parse("2026-04-06T09:30:00Z"))
                .traceId("trace-1")
                .errorCategory("TIMEOUT")
                .error(true)
                .processedAt(Instant.parse("2026-04-06T09:30:01Z"))
                .build();

        when(structuredLogMapper.map(event)).thenReturn(structuredLog);

        StructuredLog result = logProcessorService.process(event);

        assertThat(result).isEqualTo(structuredLog);
        verify(elasticsearchLogIndexer).index(structuredLog);
        verify(processedLogPublisher).publish(structuredLog);
    }
}
