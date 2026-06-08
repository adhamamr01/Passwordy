package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for creating or updating a stored password entry. {@code password} is the
 * plaintext value — it is AES-256-GCM encrypted before storage and never persisted in
 * plaintext. {@code username} is the credential's own username (e.g. the login for the
 * target site), not the Passwordy account username.
 */
public record PasswordSaveRequest(
        @NotBlank String label,
        @NotBlank String password,
        String username,
        String url,
        String notes,
        @NotBlank String category) {
}
