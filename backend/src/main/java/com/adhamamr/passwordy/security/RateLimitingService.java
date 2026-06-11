package com.adhamamr.passwordy.security;

import com.adhamamr.passwordy.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token-bucket store backing the rate-limit filter. One {@link Bucket} is created
 * lazily per (tier, key) pair and reused for that caller's subsequent requests. Buckets live
 * in a {@link ConcurrentHashMap}; this is intentionally process-local — appropriate for a
 * single-instance deployment. A distributed setup would swap this for a shared (e.g. Redis)
 * store without touching the filter.
 */
@Service
public class RateLimitingService {

    /** The request categories, each with its own limit from {@link RateLimitProperties}. */
    public enum Tier { AUTH, GENERATION, AUTHENTICATED, LOGIN_USER }

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingService(RateLimitProperties properties) {
        this.properties = properties;
    }

    /** Attempts to consume one token for {@code key} under {@code tier}, reporting the outcome. */
    public ConsumptionProbe tryConsume(Tier tier, String key) {
        Bucket bucket = buckets.computeIfAbsent(tier.name() + ":" + key, ignored -> newBucket(tier));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Account-level login throttle: consumes one token from the {@link Tier#LOGIN_USER} bucket
     * for {@code username}, returning whether the attempt is allowed. Disabled (always allowed)
     * when rate limiting is turned off, so it follows the same global switch as the filter.
     */
    public boolean tryConsumeLogin(String username) {
        if (!properties.isEnabled()) {
            return true;
        }
        return tryConsume(Tier.LOGIN_USER, username).isConsumed();
    }

    private Bucket newBucket(Tier tier) {
        RateLimitProperties.Limit limit = switch (tier) {
            case AUTH -> properties.getAuth();
            case GENERATION -> properties.getGeneration();
            case AUTHENTICATED -> properties.getAuthenticated();
            case LOGIN_USER -> properties.getLoginPerUser();
        };
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit.getCapacity())
                .refillGreedy(limit.getCapacity(), Duration.ofSeconds(limit.getRefillSeconds()))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
