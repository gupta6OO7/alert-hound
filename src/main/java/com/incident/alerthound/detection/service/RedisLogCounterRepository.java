package com.incident.alerthound.detection.service;

import com.incident.alerthound.config.AlertHoundProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisLogCounterRepository {

    private final StringRedisTemplate redisTemplate;
    private final Duration counterTtl;
    private final int windowSizeMinutes;

    public RedisLogCounterRepository(StringRedisTemplate redisTemplate, AlertHoundProperties properties) {
        this.redisTemplate = redisTemplate;
        this.counterTtl = Duration.ofMinutes(properties.detection().counterTtlMinutes());
        this.windowSizeMinutes = properties.detection().windowSizeMinutes();
    }

    public void increment(Instant timestamp, String service, boolean error) {
        String totalKey = counterKey(service, bucket(timestamp), "total");
        incrementKey(totalKey);

        if (error) {
            incrementKey(counterKey(service, bucket(timestamp), "error"));
        }
    }

    public DetectionSnapshot readWindow(String service, Instant windowEnd) {
        List<String> totalKeys = windowKeys(service, windowEnd, "total");
        List<String> errorKeys = windowKeys(service, windowEnd, "error");
        return new DetectionSnapshot(sum(totalKeys), sum(errorKeys));
    }

    public Instant windowStart(Instant windowEnd) {
        return bucket(windowEnd).minus(windowSizeMinutes - 1L, ChronoUnit.MINUTES);
    }

    private void incrementKey(String key) {
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, counterTtl);
    }

    private long sum(List<String> keys) {
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return 0L;
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .mapToLong(Long::parseLong)
                .sum();
    }

    private List<String> windowKeys(String service, Instant windowEnd, String metric) {
        Instant endBucket = bucket(windowEnd);
        Instant startBucket = endBucket.minus(windowSizeMinutes - 1L, ChronoUnit.MINUTES);
        List<String> keys = new ArrayList<>(windowSizeMinutes);
        for (Instant bucket = startBucket; !bucket.isAfter(endBucket); bucket = bucket.plus(1L, ChronoUnit.MINUTES)) {
            keys.add(counterKey(service, bucket, metric));
        }
        return keys;
    }

    private Instant bucket(Instant timestamp) {
        return timestamp.truncatedTo(ChronoUnit.MINUTES);
    }

    private String counterKey(String service, Instant bucket, String metric) {
        return "logs:%s:%s:%s".formatted(service, bucket, metric);
    }
}
