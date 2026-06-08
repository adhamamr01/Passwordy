package com.adhamamr.passwordy.dto;

import java.time.LocalDateTime;

/** Password as returned to clients; {@code value} is the encrypted ciphertext (decrypt-on-demand). */
public record PasswordResponse(
        Long id,
        String label,
        String value,
        String username,
        String url,
        String notes,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
