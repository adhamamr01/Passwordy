package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials submitted at login. {@code masterPassword} is verified against the stored Argon2id hash. */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String masterPassword) {
}
