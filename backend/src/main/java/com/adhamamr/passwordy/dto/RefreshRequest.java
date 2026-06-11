package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

/** Carries the opaque refresh token, used to obtain a new access token or to log out. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
