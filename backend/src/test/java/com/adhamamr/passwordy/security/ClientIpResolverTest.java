package com.adhamamr.passwordy.security;

import com.adhamamr.passwordy.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private ClientIpResolver resolverWithTrusted(String... trusted) {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxies(List.of(trusted));
        return new ClientIpResolver(props);
    }

    private MockHttpServletRequest request(String remoteAddr, String xff) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(remoteAddr);
        if (xff != null) {
            req.addHeader("X-Forwarded-For", xff);
        }
        return req;
    }

    @Test
    void noTrustedProxies_usesRemoteAddr_ignoringXff() {
        ClientIpResolver resolver = resolverWithTrusted();
        // Spoofed XFF must be ignored when no proxy is trusted.
        assertThat(resolver.resolve(request("203.0.113.9", "1.2.3.4"))).isEqualTo("203.0.113.9");
    }

    @Test
    void untrustedRemoteAddr_ignoresXff() {
        ClientIpResolver resolver = resolverWithTrusted("10.0.0.1");
        // Direct peer is not the trusted proxy → header is attacker-controlled, ignore it.
        assertThat(resolver.resolve(request("203.0.113.9", "1.2.3.4"))).isEqualTo("203.0.113.9");
    }

    @Test
    void trustedProxy_returnsForwardedClient() {
        ClientIpResolver resolver = resolverWithTrusted("10.0.0.1");
        assertThat(resolver.resolve(request("10.0.0.1", "198.51.100.7"))).isEqualTo("198.51.100.7");
    }

    @Test
    void trustedProxy_returnsRightmostNonTrustedHop() {
        ClientIpResolver resolver = resolverWithTrusted("10.0.0.1", "10.0.0.2");
        // Chain: realClient, then two trusted proxies. Walk right-to-left past trusted ones.
        assertThat(resolver.resolve(request("10.0.0.1", "198.51.100.7, 10.0.0.2")))
                .isEqualTo("198.51.100.7");
    }

    @Test
    void trustedProxy_noXffHeader_fallsBackToRemoteAddr() {
        ClientIpResolver resolver = resolverWithTrusted("10.0.0.1");
        assertThat(resolver.resolve(request("10.0.0.1", null))).isEqualTo("10.0.0.1");
    }
}
