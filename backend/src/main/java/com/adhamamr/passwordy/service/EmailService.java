package com.adhamamr.passwordy.service;

/**
 * Sends transactional emails. Abstracted so the delivery mechanism (SMTP today) can change
 * without touching the auth logic, and so tests can substitute a mock.
 */
public interface EmailService {

    /** Sends the account-verification link carrying {@code token} to {@code toEmail}. */
    void sendVerificationEmail(String toEmail, String token);

    /** Sends the password-reset link carrying {@code token} to {@code toEmail}. */
    void sendPasswordResetEmail(String toEmail, String token);
}
