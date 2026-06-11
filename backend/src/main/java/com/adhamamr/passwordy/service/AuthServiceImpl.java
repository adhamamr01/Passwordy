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
import com.adhamamr.passwordy.exception.BadRequestException;
import com.adhamamr.passwordy.exception.EmailNotVerifiedException;
import com.adhamamr.passwordy.exception.InvalidCredentialsException;
import com.adhamamr.passwordy.exception.ResourceNotFoundException;
import com.adhamamr.passwordy.exception.TooManyRequestsException;
import com.adhamamr.passwordy.model.RecoveryCode;
import com.adhamamr.passwordy.model.TokenPurpose;
import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.model.VerificationToken;
import com.adhamamr.passwordy.repository.RecoveryCodeRepository;
import com.adhamamr.passwordy.repository.UserRepository;
import com.adhamamr.passwordy.repository.VerificationTokenRepository;
import com.adhamamr.passwordy.security.JwtUtil;
import com.adhamamr.passwordy.security.RateLimitingService;
import com.adhamamr.passwordy.util.MasterPasswordValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
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
    private static final String EMAIL_ACK =
            "If an account with that email exists, we've sent it an email. Please check your inbox.";

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RateLimitingService rateLimitingService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final TotpService totpService;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final EncryptionService encryptionService;
    private final SecureRandom secureRandom = new SecureRandom();
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
                           RefreshTokenService refreshTokenService,
                           TotpService totpService,
                           RecoveryCodeRepository recoveryCodeRepository,
                           EncryptionService encryptionService,
                           @Value("${app.verification.token-ttl-hours:24}") long tokenTtlHours) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rateLimitingService = rateLimitingService;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
        this.totpService = totpService;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.encryptionService = encryptionService;
        this.tokenTtl = Duration.ofHours(tokenTtlHours);
        this.dummyHash = passwordEncoder.encode("timing-guard-not-a-real-password");
    }

    private AuthResponse issueTokens(User user, String message) {
        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getEmail(),
                message, false, null);
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

            String token = issueToken(savedUser, TokenPurpose.VERIFY_EMAIL);
            emailService.sendVerificationEmail(savedUser.getEmail(), token);
        }

        return new MessageResponse(REGISTER_ACK);
    }

    @Override
    @Transactional
    public MessageResponse verify(String token) {
        VerificationToken verificationToken = consumeToken(token, TokenPurpose.VERIFY_EMAIL);

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);

        return new MessageResponse("Email verified. You can now log in.");
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        // Enumeration-safe: only act if the account exists, but always return the same ack.
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = issueToken(user, TokenPurpose.PASSWORD_RESET);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
        return new MessageResponse(EMAIL_ACK);
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        MasterPasswordValidator.ValidationResult validation =
                MasterPasswordValidator.validate(request.newPassword());
        if (!validation.isValid()) {
            throw new BadRequestException(validation.getErrorMessage());
        }

        VerificationToken token = consumeToken(request.token(), TokenPurpose.PASSWORD_RESET);
        User user = token.getUser();
        user.setMasterPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setEnabled(true); // a successful reset proves control of the email
        userRepository.save(user);
        tokenRepository.delete(token);
        refreshTokenService.revokeAll(user); // end every existing session after a reset

        return new MessageResponse("Your master password has been reset. You can now log in.");
    }

    @Override
    @Transactional
    public MessageResponse resendVerification(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email())
                .filter(user -> !user.isEnabled())
                .ifPresent(user -> {
                    String token = issueToken(user, TokenPurpose.VERIFY_EMAIL);
                    emailService.sendVerificationEmail(user.getEmail(), token);
                });
        return new MessageResponse(EMAIL_ACK);
    }

    /** Creates and persists a fresh single-use token; the caller sends the matching email. */
    private String issueToken(User user, TokenPurpose purpose) {
        String token = UUID.randomUUID().toString();
        tokenRepository.save(new VerificationToken(token, user, purpose, Instant.now().plus(tokenTtl)));
        return token;
    }

    /** Loads a token, validating it exists, matches {@code purpose}, and hasn't expired. */
    private VerificationToken consumeToken(String token, TokenPurpose purpose) {
        VerificationToken found = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));
        if (found.getPurpose() != purpose || found.isExpired()) {
            tokenRepository.delete(found);
            throw new BadRequestException("Invalid or expired token");
        }
        return found;
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

        // 2FA: password is correct, but defer token issuance to /2fa/verify.
        if (user.isTotpEnabled()) {
            String twoFactorToken = jwtUtil.generateTwoFactorToken(user.getUsername());
            return new AuthResponse(null, null, user.getUsername(), user.getEmail(),
                    "Two-factor authentication required", true, twoFactorToken);
        }

        return issueTokens(user, "Login successful");
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        User user = refreshTokenService.rotate(request.refreshToken());
        return issueTokens(user, "Token refreshed");
    }

    @Override
    @Transactional
    public MessageResponse logout(RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return new MessageResponse("Logged out.");
    }

    // --- two-factor (TOTP) ---

    @Override
    @Transactional
    public TotpSetupResponse setupTotp(String username) {
        User user = requireUser(username);
        String secret = totpService.generateSecret();
        user.setTotpSecret(encrypt(secret));   // stored encrypted; not active until enableTotp
        userRepository.save(user);
        return new TotpSetupResponse(secret, totpService.otpAuthUri(secret, username));
    }

    @Override
    @Transactional
    public TotpEnableResponse enableTotp(String username, String code) {
        User user = requireUser(username);
        if (user.getTotpSecret() == null) {
            throw new BadRequestException("Start 2FA setup first");
        }
        if (!totpService.verify(decrypt(user.getTotpSecret()), code)) {
            throw new BadRequestException("Invalid verification code");
        }
        user.setTotpEnabled(true);
        userRepository.save(user);

        recoveryCodeRepository.deleteByUser(user);
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String raw = randomRecoveryCode();
            codes.add(raw);
            recoveryCodeRepository.save(new RecoveryCode(sha256(raw), user));
        }
        return new TotpEnableResponse(codes, "Two-factor authentication enabled. Save these recovery codes.");
    }

    @Override
    @Transactional
    public MessageResponse disableTotp(String username, String code) {
        User user = requireUser(username);
        if (!user.isTotpEnabled() || !totpService.verify(decrypt(user.getTotpSecret()), code)) {
            throw new BadRequestException("Invalid verification code");
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        recoveryCodeRepository.deleteByUser(user);
        return new MessageResponse("Two-factor authentication disabled.");
    }

    @Override
    @Transactional
    public AuthResponse verifyTwoFactor(TwoFactorVerifyRequest request) {
        if (!jwtUtil.isTwoFactorToken(request.twoFactorToken())) {
            throw new InvalidCredentialsException("Invalid or expired two-factor session");
        }
        String username = jwtUtil.extractUsername(request.twoFactorToken());
        User user = userRepository.findByUsername(username)
                .filter(User::isTotpEnabled)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid two-factor session"));

        boolean ok = totpService.verify(decrypt(user.getTotpSecret()), request.code())
                || consumeRecoveryCode(user, request.code());
        if (!ok) {
            throw new InvalidCredentialsException("Invalid two-factor code");
        }
        return issueTokens(user, "Login successful");
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean consumeRecoveryCode(User user, String code) {
        return recoveryCodeRepository.findByUserAndCodeHash(user, sha256(code))
                .map(rc -> { recoveryCodeRepository.delete(rc); return true; })
                .orElse(false);
    }

    private String randomRecoveryCode() {
        byte[] bytes = new byte[6];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes); // 12 hex chars
    }

    private String encrypt(String value) {
        try {
            return encryptionService.encrypt(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt 2FA secret", e);
        }
    }

    private String decrypt(String value) {
        try {
            return encryptionService.decrypt(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt 2FA secret", e);
        }
    }

    private String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
