package com.adhamamr.passwordy.security;

import com.adhamamr.passwordy.config.RateLimitProperties;
import com.adhamamr.passwordy.security.RateLimitingService.Tier;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private RateLimitBucketProvider bucketProvider;

    private RateLimitingService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitingService(new RateLimitProperties(), bucketProvider);
    }

    @Test
    void failsOpenWhenStoreThrows() {
        when(bucketProvider.resolve(anyString(), any())).thenThrow(new RuntimeException("store down"));

        // Store outage must not reject traffic — both paths allow.
        assertThat(service.tryConsume(Tier.AUTH, "ip:1.2.3.4").isConsumed()).isTrue();
        assertThat(service.tryConsumeLogin("alice")).isTrue();
    }

    @Test
    void delegatesToStoreWhenAvailable() {
        // Real in-memory bucket built from the supplied bandwidth (default auth capacity = 5).
        when(bucketProvider.resolve(anyString(), any())).thenAnswer(invocation ->
                Bucket.builder().addLimit((Bandwidth) invocation.getArgument(1)).build());

        assertThat(service.tryConsume(Tier.AUTH, "ip:5.6.7.8").isConsumed()).isTrue();
    }
}
