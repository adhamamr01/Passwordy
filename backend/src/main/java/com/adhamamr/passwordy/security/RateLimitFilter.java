package com.adhamamr.passwordy.security;

import com.adhamamr.passwordy.config.RateLimitProperties;
import com.adhamamr.passwordy.security.RateLimitingService.Tier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Throttles every {@code /api/**} request through a tiered token bucket before it reaches the
 * controller. Runs after {@link JwtAuthenticationFilter} so the authenticated tier can key by
 * username (set in the security context); auth and generation tiers — and any unauthenticated
 * request — fall back to the client IP. On exhaustion it short-circuits with HTTP 429 and a
 * {@code Retry-After} header, using the same {@code {"error": "..."}} body as the rest of the API.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimitingService rateLimitingService;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitProperties properties,
                           RateLimitingService rateLimitingService,
                           ClientIpResolver clientIpResolver) {
        this.properties = properties;
        this.rateLimitingService = rateLimitingService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled() || !request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Tier tier = resolveTier(request);
        ConsumptionProbe probe = rateLimitingService.tryConsume(tier, resolveKey(request, tier));

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Map.of("error", "Too many requests, please try again later"));
    }

    private Tier resolveTier(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/")) {
            return Tier.AUTH;
        }
        if (uri.equals("/api/password/generate") || uri.equals("/api/password/generate-pin")) {
            return Tier.GENERATION;
        }
        return Tier.AUTHENTICATED;
    }

    private String resolveKey(HttpServletRequest request, Tier tier) {
        if (tier == Tier.AUTHENTICATED) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken) && auth.getName() != null) {
                return "user:" + auth.getName();
            }
        }
        return "ip:" + clientIpResolver.resolve(request);
    }
}
