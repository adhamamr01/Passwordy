package com.adhamamr.passwordy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Calls the HIBP range API over HTTPS. Sends {@code Add-Padding: true} so the response size can't
 * hint at how many suffixes matched. Any failure (timeout, IO, non-200) returns
 * {@link Optional#empty()} — the caller then fails open, so HIBP being down never blocks a user.
 */
@Component
public class HttpPwnedRangeClient implements PwnedRangeClient {

    private static final Logger log = LoggerFactory.getLogger(HttpPwnedRangeClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final String apiUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public HttpPwnedRangeClient(@Value("${breachcheck.api-url:https://api.pwnedpasswords.com}") String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @Override
    public Optional<String> fetchRange(String hashPrefix5) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/range/" + hashPrefix5))
                    .header("Add-Padding", "true")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Optional.of(response.body());
            }
            log.warn("HIBP range lookup returned status {}", response.statusCode());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("HIBP range lookup failed (failing open): {}", e.getMessage());
            return Optional.empty();
        }
    }
}
