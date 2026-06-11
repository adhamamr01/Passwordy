package com.adhamamr.passwordy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized rate-limit configuration, bound from {@code ratelimit.*} properties.
 *
 * <p>Tiers map to the request categories enforced by the filter: {@code auth} (login /
 * register, keyed by IP), {@code generation} (public password/PIN generation, keyed by IP),
 * and {@code authenticated} (per-user CRUD, keyed by username). {@code loginPerUser} is an
 * additional account-level tier applied in the service layer, keyed by the submitted login
 * username, so a targeted brute-force is throttled even across rotating IPs. Each tier defines
 * a token bucket {@code capacity} that fully refills every {@code refillSeconds}.
 *
 * <p>{@code enabled} defaults to true; tests flip it off so their repeated requests don't trip
 * the limiter. {@code trustedProxies} is the set of front-proxy IPs whose {@code X-Forwarded-For}
 * header may be trusted to recover the real client IP; empty (the default) means trust none and
 * use the direct socket address.
 */
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Limit auth = new Limit(5, 60);
    private Limit generation = new Limit(20, 60);
    private Limit authenticated = new Limit(100, 60);
    private Limit loginPerUser = new Limit(5, 60);
    private List<String> trustedProxies = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Limit getAuth() { return auth; }
    public void setAuth(Limit auth) { this.auth = auth; }

    public Limit getGeneration() { return generation; }
    public void setGeneration(Limit generation) { this.generation = generation; }

    public Limit getAuthenticated() { return authenticated; }
    public void setAuthenticated(Limit authenticated) { this.authenticated = authenticated; }

    public Limit getLoginPerUser() { return loginPerUser; }
    public void setLoginPerUser(Limit loginPerUser) { this.loginPerUser = loginPerUser; }

    public List<String> getTrustedProxies() { return trustedProxies; }
    public void setTrustedProxies(List<String> trustedProxies) { this.trustedProxies = trustedProxies; }

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
