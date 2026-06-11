package com.adhamamr.passwordy.dto;

/**
 * Returned when starting 2FA setup: the base32 {@code secret} (for manual entry) and the
 * {@code otpauthUri} the client renders as a QR code. 2FA isn't active until confirmed.
 */
public record TotpSetupResponse(String secret, String otpauthUri) {
}
