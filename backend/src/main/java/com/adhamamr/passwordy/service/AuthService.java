package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.dto.RegisterRequest;

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
}
