package com.adhamamr.passwordy.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP used as a rate-limit key. Currently returns the raw TCP source
 * address ({@code getRemoteAddr()}), which cannot be spoofed and is correct for the direct
 * (no-proxy) deployment. This is the single swap-point: if a trusted reverse proxy is ever
 * introduced, recover the real client from {@code X-Forwarded-For} here — but only when the
 * connection originates from a known proxy, since the header is otherwise attacker-controlled.
 */
@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
