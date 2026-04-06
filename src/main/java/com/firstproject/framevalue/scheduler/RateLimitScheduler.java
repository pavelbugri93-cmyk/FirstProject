package com.firstproject.framevalue.scheduler;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimitScheduler - Prevents spam by limiting requests to 10 per second per IP.
 * Cleans inactive buckets hourly using Bucket4j token bucket algorithm.
 */

@Component
@Slf4j
public class RateLimitScheduler {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                10,
                Refill.intervally(10, Duration.ofSeconds(1))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String ip) {
        Bucket bucket = resolveBucket(ip);
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded for IP: {}", ip);
        }

        return allowed;
    }

    @Scheduled(cron = "0 0 * * * ?")  // Every hour
    public void cleanupInactiveBuckets() {
        int sizeBefore = buckets.size();

        buckets.clear();

        log.info("Rate limit buckets cleaned: {} buckets removed", sizeBefore);
    }
}