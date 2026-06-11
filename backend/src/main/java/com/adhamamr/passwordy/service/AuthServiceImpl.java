package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.RegisterRequest;
import com.adhamamr.passwordy.exception.BadRequestException;
import com.adhamamr.passwordy.exception.InvalidCredentialsException;
import com.adhamamr.passwordy.exception.TooManyRequestsException;
import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.repository.UserRepository;
import com.adhamamr.passwordy.security.JwtUtil;
import com.adhamamr.passwordy.security.RateLimitingService;
import com.adhamamr.passwordy.util.MasterPasswordValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login logic.
 *
 * <p>Registration validates master-password strength, rejects duplicate username/email,
 * stores only the Argon2id hash of the master password, and returns a JWT. Login verifies
 * the supplied master password against the stored hash (never decrypting) and issues a JWT.
 * Existing BCrypt hashes are transparently upgraded to Argon2id on first successful login
 * (lazy rehash migration).
 *
 * <p>Login is additionally throttled per account via {@link RateLimitingService}: this
 * complements the filter's IP tier so that brute-forcing one account is capped even when the
 * attacker rotates source IPs.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RateLimitingService rateLimitingService;

    /**
     * A throwaway Argon2id hash used to spend the same time hashing when an unknown username
     * is supplied at login, so the response time can't reveal whether the account exists
     * (timing-based enumeration guard).
     */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           RateLimitingService rateLimitingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rateLimitingService = rateLimitingService;
        this.dummyHash = passwordEncoder.encode("timing-guard-not-a-real-password");
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        MasterPasswordValidator.ValidationResult validation =
                MasterPasswordValidator.validate(request.masterPassword());
        if (!validation.isValid()) {
            throw new BadRequestException(validation.getErrorMessage());
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setMasterPasswordHash(passwordEncoder.encode(request.masterPassword()));

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser.getUsername());

        return new AuthResponse(token, savedUser.getUsername(), savedUser.getEmail(), "User registered successfully");
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

        // Lazy rehash: upgrade BCrypt hashes to Argon2id on first successful login.
        if (passwordEncoder.upgradeEncoding(user.getMasterPasswordHash())) {
            user.setMasterPasswordHash(passwordEncoder.encode(request.masterPassword()));
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getEmail(), "Login successful");
    }
}
