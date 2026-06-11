package com.adhamamr.passwordy.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Supplies the {@link Bucket} for a given rate-limit key. The implementation decides where the
 * bucket state lives: process-local (in-memory) or shared (Redis). {@link RateLimitingService}
 * and the filter only ever see a {@link Bucket}, so swapping the store changes nothing upstream.
 */
public interface RateLimitBucketProvider {

    /** Returns the bucket for {@code key}, creating it from {@code bandwidth} on first use. */
    Bucket resolve(String key, Bandwidth bandwidth);
}
