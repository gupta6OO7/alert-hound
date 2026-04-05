package com.incident.alerthound.logprocessor.service;

import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import org.springframework.stereotype.Service;

@Service
public class LogProcessorService {

    private final StructuredLogMapper structuredLogMapper;
    private final ElasticsearchLogIndexer elasticsearchLogIndexer;
    private final ProcessedLogPublisher processedLogPublisher;

    public LogProcessorService(
            StructuredLogMapper structuredLogMapper,
            ElasticsearchLogIndexer elasticsearchLogIndexer,
            ProcessedLogPublisher processedLogPublisher
    ) {
        this.structuredLogMapper = structuredLogMapper;
        this.elasticsearchLogIndexer = elasticsearchLogIndexer;
        this.processedLogPublisher = processedLogPublisher;
    }

    public StructuredLog process(LogEvent event) {
        StructuredLog structuredLog = structuredLogMapper.map(event);
        elasticsearchLogIndexer.index(structuredLog);
        processedLogPublisher.publish(structuredLog);
        return structuredLog;
    }
}
