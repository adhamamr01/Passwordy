package com.adhamamr.passwordy.security;

import com.adhamamr.passwordy.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token-bucket rate limiting. Resolves one {@link Bucket} per (tier, key) through a
 * {@link RateLimitBucketProvider} — process-local by default, or Redis-backed (shared across
 * instances) when {@code ratelimit.store=redis}. The store is invisible here and to the filter.
 */
@Service
public class RateLimitingService {

    /** The request categories, each with its own limit from {@link RateLimitProperties}. */
    public enum Tier { AUTH, GENERATION, AUTHENTICATED, LOGIN_USER }

    private final RateLimitProperties properties;
    private final RateLimitBucketProvider bucketProvider;

    public RateLimitingService(RateLimitProperties properties, RateLimitBucketProvider bucketProvider) {
        this.properties = properties;
        this.bucketProvider = bucketProvider;
    }

    /** Attempts to consume one token for {@code key} under {@code tier}, reporting the outcome. */
    public ConsumptionProbe tryConsume(Tier tier, String key) {
        Bucket bucket = bucketProvider.resolve(tier.name() + ":" + key, bandwidthFor(tier));
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

    private Bandwidth bandwidthFor(Tier tier) {
        RateLimitProperties.Limit limit = switch (tier) {
            case AUTH -> properties.getAuth();
            case GENERATION -> properties.getGeneration();
            case AUTHENTICATED -> properties.getAuthenticated();
            case LOGIN_USER -> properties.getLoginPerUser();
        };
        return Bandwidth.builder()
                .capacity(limit.getCapacity())
                .refillGreedy(limit.getCapacity(), Duration.ofSeconds(limit.getRefillSeconds()))
                .build();
    }
}
