package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

/** A 6-digit TOTP code, used to enable or disable 2FA. */
public record TotpCodeRequest(@NotBlank String code) {
}
