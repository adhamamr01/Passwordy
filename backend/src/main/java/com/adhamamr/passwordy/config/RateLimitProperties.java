package com.adhamamr.passwordy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized rate-limit configuration, bound from {@code ratelimit.*} properties.
 *
 * <p>Three tiers map to the request categories enforced by the filter: {@code auth} (login /
 * register, keyed by IP), {@code generation} (public password/PIN generation, keyed by IP),
 * and {@code authenticated} (per-user CRUD, keyed by username). Each tier defines a token
 * bucket {@code capacity} that fully refills every {@code refillSeconds}. {@code enabled}
 * defaults to true; tests flip it off so their repeated requests don't trip the limiter.
 */
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Limit auth = new Limit(5, 60);
    private Limit generation = new Limit(20, 60);
    private Limit authenticated = new Limit(100, 60);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Limit getAuth() { return auth; }
    public void setAuth(Limit auth) { this.auth = auth; }

    public Limit getGeneration() { return generation; }
    public void setGeneration(Limit generation) { this.generation = generation; }

    public Limit getAuthenticated() { return authenticated; }
    public void setAuthenticated(Limit authenticated) { this.authenticated = authenticated; }

    /** A single tier's bucket size ({@code capacity}) and full-refill interval ({@code refillSeconds}). */
    public static class Limit {
        private int capacity;
        private long refillSeconds;

        public Limit() {}

        public Limit(int capacity, long refillSeconds) {
            this.capacity = capacity;
            this.refillSeconds = refillSeconds;
        }

        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }

        public long getRefillSeconds() { return refillSeconds; }
        public void setRefillSeconds(long refillSeconds) { this.refillSeconds = refillSeconds; }
    }
}
