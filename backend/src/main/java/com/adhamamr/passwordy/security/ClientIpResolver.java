package com.adhamamr.passwordy.security;

import com.adhamamr.passwordy.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the client IP used as a rate-limit key.
 *
 * <p>By default ({@code ratelimit.trusted-proxies} empty) it returns the raw TCP source
 * address ({@code getRemoteAddr()}), which cannot be spoofed and is correct for a direct,
 * no-proxy deployment.
 *
 * <p>When deployed behind a reverse proxy / load balancer, list that proxy's address in
 * {@code ratelimit.trusted-proxies}. The {@code X-Forwarded-For} header is then consulted
 * <em>only</em> when the direct peer is itself a trusted proxy — otherwise the header is
 * attacker-controlled and ignored. Among the forwarded entries we walk right-to-left and
 * take the first address that is not a trusted proxy, i.e. the real client as seen at the
 * trust boundary; spoofed entries an attacker prepends are below that boundary and discarded.
 */
@Component
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final RateLimitProperties properties;

    public ClientIpResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        List<String> trustedProxies = properties.getTrustedProxies();

        if (trustedProxies.isEmpty() || !trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }

        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (forwarded == null || forwarded.isBlank()) {
            return remoteAddr;
        }

        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!hop.isEmpty() && !trustedProxies.contains(hop)) {
                return hop;
            }
        }
        return remoteAddr;
    }
}
