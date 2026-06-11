package com.adhamamr.passwordy.dto;

/**
 * Returned on successful login or token refresh. {@code token} is a short-lived access JWT;
 * {@code refreshToken} is the long-lived opaque token for {@code /api/auth/refresh}.
 *
 * <p>When the account has 2FA enabled, login returns {@code twoFactorRequired = true} with a
 * short-lived {@code twoFactorToken} and <b>no</b> access/refresh tokens — the client completes
 * login at {@code /api/auth/2fa/verify}.
 */
public record AuthResponse(
        String token,
        String refreshToken,
        String username,
        String email,
        String message,
        boolean twoFactorRequired,
        String twoFactorToken) {
}
