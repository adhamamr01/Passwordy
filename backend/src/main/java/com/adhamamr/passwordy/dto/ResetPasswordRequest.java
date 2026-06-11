package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

/** Completes a password reset: the emailed token plus the new master password. */
public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {
}
