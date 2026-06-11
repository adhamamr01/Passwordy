package com.adhamamr.passwordy.dto;

import java.time.Instant;

/** Password as returned to clients; {@code value} is the encrypted ciphertext (decrypt-on-demand). */
public record PasswordResponse(
        Long id,
        String label,
        String value,
        String username,
        String url,
        String notes,
        String category,
        boolean favorite,
        Instant createdAt,
        Instant updatedAt) {
}
