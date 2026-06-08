package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for new account registration. {@code username} and {@code email} must each be
 * unique. {@code masterPassword} is validated for strength by {@link com.adhamamr.passwordy.util.MasterPasswordValidator}
 * before being Argon2id-hashed and stored — the plaintext is never persisted.
 */
public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String masterPassword) {
}
