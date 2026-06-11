package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login step 2: the short-lived {@code twoFactorToken} from step 1 plus a {@code code} — either
 * a 6-digit TOTP code or a one-time recovery code.
 */
public record TwoFactorVerifyRequest(@NotBlank String twoFactorToken, @NotBlank String code) {
}
