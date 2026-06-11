package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.ForgotPasswordRequest;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.dto.RefreshRequest;
import com.adhamamr.passwordy.dto.RegisterRequest;
import com.adhamamr.passwordy.dto.ResetPasswordRequest;

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
}
