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
        String link = baseUrl + "/api/auth/verify?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Verify your Passwordy account");
        message.setText("Welcome to Passwordy!\n\nPlease verify your account by opening this link:\n"
                + link + "\n\nIf you didn't create this account, you can ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send verification email to {} (registration still proceeds)", toEmail, e);
        }
    }
}
