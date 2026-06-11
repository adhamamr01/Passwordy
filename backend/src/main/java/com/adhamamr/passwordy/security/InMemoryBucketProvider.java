package com.adhamamr.passwordy.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default, process-local bucket store — one {@link Bucket} per key in a {@link ConcurrentHashMap}.
 * Correct for a single instance; for multiple instances use the Redis-backed store
 * ({@code ratelimit.store=redis}). Active unless {@code ratelimit.store} is set to something else.
 */
@Component
@ConditionalOnProperty(name = "ratelimit.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryBucketProvider implements RateLimitBucketProvider {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Bucket resolve(String key, Bandwidth bandwidth) {
        return buckets.computeIfAbsent(key, ignored -> Bucket.builder().addLimit(bandwidth).build());
    }
}
