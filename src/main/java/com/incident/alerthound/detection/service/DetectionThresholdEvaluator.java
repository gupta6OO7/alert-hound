package com.incident.alerthound.detection.service;

import com.incident.alerthound.config.AlertHoundProperties;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class DetectionThresholdEvaluator {

    private final AlertHoundProperties.DetectionProperties properties;

    public DetectionThresholdEvaluator(AlertHoundProperties alertHoundProperties) {
        this.properties = alertHoundProperties.detection();
    }

    public DetectionDecision evaluate(DetectionSnapshot snapshot, Instant windowStart, Instant windowEnd) {
        long totalLogs = snapshot.totalLogs();
        long errorLogs = snapshot.errorLogs();
        double errorRate = snapshot.errorRate();

        if (totalLogs < properties.minimumTotalLogs()) {
            return DetectionDecision.noIncident(
                    totalLogs,
                    errorLogs,
                    errorRate,
                    windowStart,
                    windowEnd,
                    "traffic below minimum threshold"
            );
        }

        if (errorRate < properties.errorRateThreshold()) {
            return DetectionDecision.noIncident(
                    totalLogs,
                    errorLogs,
                    errorRate,
                    windowStart,
                    windowEnd,
                    "error rate below threshold"
            );
        }

        return DetectionDecision.incident(
                totalLogs,
                errorLogs,
                errorRate,
                windowStart,
                windowEnd,
                "error rate %.2f%% exceeded threshold %.2f%%".formatted(
                        errorRate * 100.0d,
                        properties.errorRateThreshold() * 100.0d
                )
        );
    }
}
