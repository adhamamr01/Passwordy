package com.adhamamr.passwordy.ratelimit;

import com.adhamamr.passwordy.security.RateLimitBucketProvider;
import com.adhamamr.passwordy.security.RateLimitingService;
import com.adhamamr.passwordy.security.RateLimitingService.Tier;
import com.adhamamr.passwordy.security.RedisBucketProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the Redis-backed bucket store end to end against a real Redis container, proving the
 * distributed path works (bucket state lives in Redis, the limiter trips at capacity). Like the
 * Postgres test, it skips when Docker is unavailable, so the default H2/in-memory suite needs
 * nothing installed; it runs for real on CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "ratelimit.enabled=true",
        "ratelimit.store=redis",
        "ratelimit.auth.capacity=3",
        "ratelimit.auth.refill-seconds=60"
})
class RedisRateLimitIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("ratelimit.redis.url",
                () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private RateLimitBucketProvider bucketProvider;

    @Test
    void redisStoreIsActive() {
        assertThat(bucketProvider).isInstanceOf(RedisBucketProvider.class);
    }

    @Test
    void limiterTripsAtCapacityBackedByRedis() {
        String key = "ip:203.0.113.50";
        // capacity = 3: first three consume, fourth is rejected — state held in Redis.
        assertThat(rateLimitingService.tryConsume(Tier.AUTH, key).isConsumed()).isTrue();
        assertThat(rateLimitingService.tryConsume(Tier.AUTH, key).isConsumed()).isTrue();
        assertThat(rateLimitingService.tryConsume(Tier.AUTH, key).isConsumed()).isTrue();
        assertThat(rateLimitingService.tryConsume(Tier.AUTH, key).isConsumed()).isFalse();
    }
}
