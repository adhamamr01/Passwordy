package com.adhamamr.passwordy.dto;

/**
 * Returned on successful register or login. {@code token} is a signed JWT the client must
 * include as {@code Authorization: Bearer <token>} on all protected requests.
 * {@code message} is a human-readable status string, not intended for programmatic use.
 */
public record AuthResponse(String token, String username, String email, String message) {
}
