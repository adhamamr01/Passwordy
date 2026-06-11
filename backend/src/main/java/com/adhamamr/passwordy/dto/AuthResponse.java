package com.adhamamr.passwordy.dto;

/**
 * Returned on successful login or token refresh. {@code token} is a short-lived access JWT the
 * client sends as {@code Authorization: Bearer <token>}; {@code refreshToken} is the long-lived
 * opaque token used at {@code /api/auth/refresh} to obtain a new access token (and at
 * {@code /api/auth/logout} to revoke the session). {@code message} is human-readable status.
 */
public record AuthResponse(String token, String refreshToken, String username, String email, String message) {
}
