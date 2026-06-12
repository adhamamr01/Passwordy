package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.ForgotPasswordRequest;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.dto.RefreshRequest;
import com.adhamamr.passwordy.dto.RegisterRequest;
import com.adhamamr.passwordy.dto.ResetPasswordRequest;
import com.adhamamr.passwordy.dto.TotpEnableResponse;
import com.adhamamr.passwordy.dto.TotpSetupResponse;
import com.adhamamr.passwordy.dto.TwoFactorVerifyRequest;

/**
 * Account registration, email verification, and login.
 *
 * <p>{@code register} never reveals whether a username/email already exists — it always returns
 * the same generic acknowledgement and issues no token; the account stays disabled until the
 * user follows the emailed verification link ({@code verify}). {@code login} issues a JWT, and
 * refuses unverified accounts (only after a correct password, so existence isn't leaked).
 */
public interface AuthService {
    MessageResponse register(RegisterRequest request);
    MessageResponse verify(String token);
    AuthResponse login(LoginRequest request);

    /** Starts a password reset (emails a reset token). Enumeration-safe: always a generic ack. */
    MessageResponse forgotPassword(ForgotPasswordRequest request);

    /** Completes a password reset with the emailed token and a new master password. */
    MessageResponse resetPassword(ResetPasswordRequest request);

    /** Re-sends a verification email if the account exists and is unverified. Generic ack. */
    MessageResponse resendVerification(ForgotPasswordRequest request);

    /** Exchanges a valid refresh token for a new access token + rotated refresh token. */
    AuthResponse refresh(RefreshRequest request);

    /** Revokes the given refresh token (logout). */
    MessageResponse logout(RefreshRequest request);

    /** Begins TOTP setup for the user: generates (but doesn't yet activate) a secret. */
    TotpSetupResponse setupTotp(String username);

    /** Activates TOTP after the user confirms a code; returns one-time recovery codes. */
    TotpEnableResponse enableTotp(String username, String code);

    /** Disables TOTP (and clears recovery codes) after confirming a current code. */
    MessageResponse disableTotp(String username, String code);

    /** Login step 2: verifies a TOTP or recovery code and issues tokens. */
    AuthResponse verifyTwoFactor(TwoFactorVerifyRequest request);

    /**
     * Permanently deletes the account and all data owned by it (passwords, refresh tokens,
     * recovery codes, outstanding verification tokens) after re-confirming the master password.
     */
    MessageResponse deleteAccount(String username, String masterPassword);
}
