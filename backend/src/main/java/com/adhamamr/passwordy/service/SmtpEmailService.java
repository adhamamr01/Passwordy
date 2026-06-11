package com.adhamamr.passwordy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * SMTP implementation of {@link EmailService} via Spring's {@link JavaMailSender} (configured
 * with {@code spring.mail.*}). The verification link is {@code app.base-url} + the verify route.
 *
 * <p>A send failure is logged but <b>not</b> rethrown: registration must return the same generic
 * response whether or not mail delivery succeeded, so a mail-server hiccup can't break (or leak
 * timing about) the flow. The user can re-request verification.
 */
@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String from;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${app.base-url}") String baseUrl,
                            @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl;
        this.from = from;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        send(toEmail, "Verify your Passwordy account",
                "Welcome to Passwordy!\n\nPlease verify your account by opening this link:\n"
                        + baseUrl + "/api/auth/verify?token=" + token
                        + "\n\nIf you didn't create this account, you can ignore this email.");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        send(toEmail, "Reset your Passwordy master password",
                "We received a request to reset your master password.\n\nUse this token to set a new "
                        + "password, or open the link:\n" + baseUrl + "/reset-password?token=" + token
                        + "\n\nToken: " + token
                        + "\n\nIf you didn't request this, you can ignore this email — your password is unchanged.");
    }

    private void send(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Don't fail (or leak timing about) the flow on a mail hiccup; the user can retry.
            log.warn("Failed to send '{}' email to {}", subject, toEmail, e);
        }
    }
}
