package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request to start a password reset (or resend verification): just the account email. */
public record ForgotPasswordRequest(@NotBlank @Email String email) {
}
