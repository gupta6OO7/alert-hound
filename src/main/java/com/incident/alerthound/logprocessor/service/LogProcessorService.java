package com.incident.alerthound.logprocessor.service;

import com.incident.alerthound.logingestion.model.LogEvent;
import com.incident.alerthound.logprocessor.model.StructuredLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogProcessorService.class);

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
        LOGGER.info(
                "Processing raw log eventId={} service={} traceId={}",
                event.id(),
                event.service(),
                event.traceId()
        );
        StructuredLog structuredLog = structuredLogMapper.map(event);
        LOGGER.debug(
                "Mapped raw log eventId={} to structured log error={} category={}",
                structuredLog.id(),
                structuredLog.error(),
                structuredLog.errorCategory()
        );
        elasticsearchLogIndexer.index(structuredLog);
        LOGGER.debug("Indexed structured log eventId={} service={}", structuredLog.id(), structuredLog.service());
        processedLogPublisher.publish(structuredLog);
        LOGGER.info("Completed log processing eventId={} service={}", structuredLog.id(), structuredLog.service());
        return structuredLog;
    }
}
