package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.dto.RegisterRequest;
import com.adhamamr.passwordy.exception.BadRequestException;
import com.adhamamr.passwordy.exception.EmailNotVerifiedException;
import com.adhamamr.passwordy.exception.InvalidCredentialsException;
import com.adhamamr.passwordy.exception.TooManyRequestsException;
import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.model.VerificationToken;
import com.adhamamr.passwordy.repository.UserRepository;
import com.adhamamr.passwordy.repository.VerificationTokenRepository;
import com.adhamamr.passwordy.security.JwtUtil;
import com.adhamamr.passwordy.security.RateLimitingService;
import com.adhamamr.passwordy.util.MasterPasswordValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Registration, email verification, and login.
 *
 * <p><b>Registration is enumeration-safe:</b> it validates master-password strength, then —
 * whether or not the username/email is already taken — returns one fixed generic message and
 * issues no token. A genuinely new account is created <em>disabled</em> with a single-use
 * verification token, emailed to the address; a duplicate is silently ignored. Either way the
 * response is identical, so {@code register} can't be used to discover which accounts exist.
 *
 * <p>{@code verify} consumes the token and enables the account. {@code login} checks the
 * password first (constant-time guard for unknown users), then refuses unverified accounts via
 * {@link EmailNotVerifiedException} — surfaced only after a correct password, so it leaks nothing
 * to an attacker. Existing BCrypt hashes are upgraded to Argon2id on first successful login. Login
 * is also throttled per account via {@link RateLimitingService}.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String REGISTER_ACK =
            "If that username and email are available, a verification link has been sent. Please check your inbox.";

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RateLimitingService rateLimitingService;
    private final EmailService emailService;
    private final Duration tokenTtl;

    /**
     * A throwaway Argon2id hash used to spend the same time hashing when an unknown username
     * is supplied at login, so the response time can't reveal whether the account exists
     * (timing-based enumeration guard).
     */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository,
                           VerificationTokenRepository tokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           RateLimitingService rateLimitingService,
                           EmailService emailService,
                           @Value("${app.verification.token-ttl-hours:24}") long tokenTtlHours) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rateLimitingService = rateLimitingService;
        this.emailService = emailService;
        this.tokenTtl = Duration.ofHours(tokenTtlHours);
        this.dummyHash = passwordEncoder.encode("timing-guard-not-a-real-password");
    }

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        MasterPasswordValidator.ValidationResult validation =
                MasterPasswordValidator.validate(request.masterPassword());
        if (!validation.isValid()) {
            throw new BadRequestException(validation.getErrorMessage());
        }

        // Enumeration-safe: a taken username/email yields the same generic response as success,
        // with no account created — the caller can't tell the difference.
        boolean taken = userRepository.existsByUsername(request.username())
                || userRepository.existsByEmail(request.email());
        if (!taken) {
            User user = new User();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setMasterPasswordHash(passwordEncoder.encode(request.masterPassword()));
            user.setEnabled(false);
            User savedUser = userRepository.save(user);

            String token = UUID.randomUUID().toString();
            tokenRepository.save(new VerificationToken(token, savedUser, Instant.now().plus(tokenTtl)));
            emailService.sendVerificationEmail(savedUser.getEmail(), token);
        }

        return new MessageResponse(REGISTER_ACK);
    }

    @Override
    @Transactional
    public MessageResponse verify(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));
        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            throw new BadRequestException("Invalid or expired verification token");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);

        return new MessageResponse("Email verified. You can now log in.");
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Account-level throttle (keyed by the submitted username), independent of source IP.
        if (!rateLimitingService.tryConsumeLogin(request.username())) {
            throw new TooManyRequestsException("Too many login attempts, please try again later");
        }

        User user = userRepository.findByUsername(request.username()).orElse(null);
        if (user == null) {
            // Hash against a dummy so an unknown username takes the same time as a wrong
            // password — otherwise response timing would reveal whether the account exists.
            passwordEncoder.matches(request.masterPassword(), dummyHash);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.masterPassword(), user.getMasterPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Only reachable with the correct password, so it reveals nothing to an attacker.
        if (!user.isEnabled()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        // Lazy rehash: upgrade BCrypt hashes to Argon2id on first successful login.
        if (passwordEncoder.upgradeEncoding(user.getMasterPasswordHash())) {
            user.setMasterPasswordHash(passwordEncoder.encode(request.masterPassword()));
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getEmail(), "Login successful");
    }
}
