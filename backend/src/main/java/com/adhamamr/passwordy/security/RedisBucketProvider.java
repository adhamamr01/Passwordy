package com.adhamamr.passwordy.security;

import com.adhamamr.passwordy.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Distributed bucket store backed by Redis (Bucket4j over Lettuce). Bucket state lives in Redis,
 * so token consumption is shared across every application instance — limits hold cluster-wide
 * instead of per-process. Active only when {@code ratelimit.store=redis}; the connection comes
 * from {@code ratelimit.redis.url}.
 *
 * <p>Each bucket's Redis key carries a write-expiration so idle entries are reclaimed.
 */
@Component
@ConditionalOnProperty(name = "ratelimit.store", havingValue = "redis")
public class RedisBucketProvider implements RateLimitBucketProvider, DisposableBean {

    private final RedisClient redisClient;
    private final StatefulRedisConnection<byte[], byte[]> connection;
    private final LettuceBasedProxyManager<byte[]> proxyManager;

    public RedisBucketProvider(RateLimitProperties properties) {
        this.redisClient = RedisClient.create(properties.getRedis().getUrl());
        this.connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(1)))
                .build();
    }

    @Override
    public Bucket resolve(String key, Bandwidth bandwidth) {
        BucketConfiguration configuration = BucketConfiguration.builder().addLimit(bandwidth).build();
        return proxyManager.builder()
                .build(key.getBytes(StandardCharsets.UTF_8), () -> configuration);
    }

    @Override
    public void destroy() {
        connection.close();
        redisClient.shutdown();
    }
}
