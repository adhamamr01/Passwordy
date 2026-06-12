package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.ForgotPasswordRequest;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.dto.RefreshRequest;
import com.adhamamr.passwordy.dto.RegisterRequest;
import com.adhamamr.passwordy.dto.ResetPasswordRequest;
import com.adhamamr.passwordy.dto.TotpEnableResponse;
import com.adhamamr.passwordy.dto.TwoFactorVerifyRequest;
import com.adhamamr.passwordy.model.RecoveryCode;
import com.adhamamr.passwordy.model.TokenPurpose;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock VerificationTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock RateLimitingService rateLimitingService;
    @Mock EmailService emailService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock TotpService totpService;
    @Mock com.adhamamr.passwordy.repository.RecoveryCodeRepository recoveryCodeRepository;
    @Mock com.adhamamr.passwordy.repository.PasswordRepository passwordRepository;
    @Mock EncryptionService encryptionService;
    @Mock BreachCheckService breachCheckService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
        lenient().when(rateLimitingService.tryConsumeLogin(anyString())).thenReturn(true);
        lenient().when(breachCheckService.isBreached(anyString())).thenReturn(false);
        authService = new AuthServiceImpl(userRepository, tokenRepository, passwordEncoder, jwtUtil,
                rateLimitingService, emailService, refreshTokenService, totpService,
                recoveryCodeRepository, passwordRepository, encryptionService, breachCheckService, 24L);
    }

    private User verifiedUser() {
        User u = new User("alice", "alice@example.com", "$hashed$");
        u.setEnabled(true);
        return u;
    }

    // --- register ---

    @Test
    void register_newAccount_createsDisabledUserAndSendsEmail() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.register(
                new RegisterRequest("alice", "alice@example.com", "StrongP@ss1"));

        assertThat(response.message()).contains("verification link");
        verify(userRepository).save(argThat(u -> !u.isEnabled()));
        verify(tokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendVerificationEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void register_breachedPassword_throwsBadRequestAndDoesNothing() {
        when(breachCheckService.isBreached("StrongP@ss1")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "alice@example.com", "StrongP@ss1")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("data breaches");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void register_weakPassword_throwsBadRequestAndDoesNothing() {
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "alice@example.com", "weak")))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void register_existingUsername_returnsGenericAckWithoutLeakingOrCreating() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        MessageResponse response = authService.register(
                new RegisterRequest("alice", "alice@example.com", "StrongP@ss1"));

        // Identical to the success message; no account created, no email sent → no oracle.
        assertThat(response.message()).contains("verification link");
        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void register_existingEmail_returnsGenericAckWithoutCreating() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        MessageResponse response = authService.register(
                new RegisterRequest("alice", "alice@example.com", "StrongP@ss1"));

        assertThat(response.message()).contains("verification link");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    // --- verify ---

    @Test
    void verify_validToken_enablesUserAndConsumesToken() {
        User user = new User("alice", "alice@example.com", "$hashed$");
        VerificationToken token = new VerificationToken("tok-123", user, TokenPurpose.VERIFY_EMAIL, Instant.now().plusSeconds(3600));
        when(tokenRepository.findByToken("tok-123")).thenReturn(Optional.of(token));

        MessageResponse response = authService.verify("tok-123");

        assertThat(response.message()).contains("verified");
        assertThat(user.isEnabled()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    void verify_unknownToken_throwsBadRequest() {
        when(tokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verify("nope"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void verify_expiredToken_throwsBadRequestAndDeletesToken() {
        User user = new User("alice", "alice@example.com", "$hashed$");
        VerificationToken token = new VerificationToken("old", user, TokenPurpose.VERIFY_EMAIL, Instant.now().minusSeconds(1));
        when(tokenRepository.findByToken("old")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verify("old"))
                .isInstanceOf(BadRequestException.class);
        verify(tokenRepository).delete(token);
        verify(userRepository, never()).save(any());
    }

    // --- forgot / reset / resend ---

    @Test
    void forgotPassword_existingEmail_issuesResetTokenAndEmails() {
        User user = verifiedUser();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        MessageResponse response = authService.forgotPassword(new ForgotPasswordRequest("alice@example.com"));

        assertThat(response.message()).contains("email");
        verify(tokenRepository).save(argThat(t -> t.getPurpose() == TokenPurpose.PASSWORD_RESET));
        verify(emailService).sendPasswordResetEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void forgotPassword_unknownEmail_returnsGenericAckWithoutEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(new ForgotPasswordRequest("ghost@example.com"));

        assertThat(response.message()).contains("email");
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void resetPassword_validToken_rehashesEnablesAndConsumes() {
        User user = new User("alice", "alice@example.com", "$old$");
        VerificationToken token = new VerificationToken("rst", user, TokenPurpose.PASSWORD_RESET, Instant.now().plusSeconds(3600));
        when(tokenRepository.findByToken("rst")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewStr0ng@1")).thenReturn("$argon2new$");

        MessageResponse response = authService.resetPassword(new ResetPasswordRequest("rst", "NewStr0ng@1"));

        assertThat(response.message()).contains("reset");
        assertThat(user.getMasterPasswordHash()).isEqualTo("$argon2new$");
        assertThat(user.isEnabled()).isTrue();
        verify(tokenRepository).delete(token);
        verify(refreshTokenService).revokeAll(user); // reset ends all existing sessions
    }

    @Test
    void resetPassword_wrongPurposeToken_throwsBadRequest() {
        User user = new User("alice", "alice@example.com", "$old$");
        VerificationToken token = new VerificationToken("verify-tok", user, TokenPurpose.VERIFY_EMAIL, Instant.now().plusSeconds(3600));
        when(tokenRepository.findByToken("verify-tok")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("verify-tok", "NewStr0ng@1")))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_weakNewPassword_throwsBadRequestBeforeTokenLookup() {
        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("rst", "weak")))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void resendVerification_unverifiedAccount_emailsNewToken() {
        User user = new User("alice", "alice@example.com", "$hashed$"); // enabled = false
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification(new ForgotPasswordRequest("alice@example.com"));

        verify(emailService).sendVerificationEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void resendVerification_alreadyVerified_doesNothing() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(verifiedUser()));

        authService.resendVerification(new ForgotPasswordRequest("alice@example.com"));

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    // --- login ---

    @Test
    void login_verifiedCredentials_returnsAccessAndRefreshTokens() {
        User user = verifiedUser();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongP@ss1", "$hashed$")).thenReturn(true);
        when(passwordEncoder.upgradeEncoding("$hashed$")).thenReturn(false);
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");
        when(refreshTokenService.issue(user)).thenReturn("refresh-xyz");

        AuthResponse response = authService.login(new LoginRequest("alice", "StrongP@ss1"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-xyz");
    }

    @Test
    void refresh_validToken_rotatesAndIssuesNewPair() {
        User user = verifiedUser();
        when(refreshTokenService.rotate("old-refresh")).thenReturn(user);
        when(jwtUtil.generateToken("alice")).thenReturn("new-access");
        when(refreshTokenService.issue(user)).thenReturn("new-refresh");

        AuthResponse response = authService.refresh(new RefreshRequest("old-refresh"));

        assertThat(response.token()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void logout_revokesRefreshToken() {
        MessageResponse response = authService.logout(new RefreshRequest("rt"));

        assertThat(response.message()).contains("Logged out");
        verify(refreshTokenService).revoke("rt");
    }

    // --- two-factor ---

    @Test
    void login_with2faEnabled_returnsChallengeNotTokens() {
        User user = verifiedUser();
        user.setTotpEnabled(true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongP@ss1", "$hashed$")).thenReturn(true);
        when(passwordEncoder.upgradeEncoding("$hashed$")).thenReturn(false);
        when(jwtUtil.generateTwoFactorToken("alice")).thenReturn("2fa-token");

        AuthResponse response = authService.login(new LoginRequest("alice", "StrongP@ss1"));

        assertThat(response.twoFactorRequired()).isTrue();
        assertThat(response.twoFactorToken()).isEqualTo("2fa-token");
        assertThat(response.token()).isNull();
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void verifyTwoFactor_validTotpCode_issuesTokens() throws Exception {
        User user = verifiedUser();
        user.setTotpEnabled(true);
        user.setTotpSecret("enc-secret");
        when(jwtUtil.isTwoFactorToken("2fa-token")).thenReturn(true);
        when(jwtUtil.extractUsername("2fa-token")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(encryptionService.decrypt("enc-secret")).thenReturn("SECRET");
        when(totpService.verify("SECRET", "123456")).thenReturn(true);
        when(jwtUtil.generateToken("alice")).thenReturn("access");
        when(refreshTokenService.issue(user)).thenReturn("refresh");

        AuthResponse response = authService.verifyTwoFactor(new TwoFactorVerifyRequest("2fa-token", "123456"));

        assertThat(response.token()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void verifyTwoFactor_validRecoveryCode_issuesTokens() throws Exception {
        User user = verifiedUser();
        user.setTotpEnabled(true);
        user.setTotpSecret("enc-secret");
        when(jwtUtil.isTwoFactorToken("2fa-token")).thenReturn(true);
        when(jwtUtil.extractUsername("2fa-token")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(encryptionService.decrypt("enc-secret")).thenReturn("SECRET");
        when(totpService.verify(eq("SECRET"), anyString())).thenReturn(false);
        when(recoveryCodeRepository.findByUserAndCodeHash(eq(user), anyString()))
                .thenReturn(Optional.of(new RecoveryCode("hash", user)));
        when(jwtUtil.generateToken("alice")).thenReturn("access");
        when(refreshTokenService.issue(user)).thenReturn("refresh");

        AuthResponse response = authService.verifyTwoFactor(new TwoFactorVerifyRequest("2fa-token", "BACKUP1"));

        assertThat(response.token()).isEqualTo("access");
        verify(recoveryCodeRepository).delete(any(RecoveryCode.class));
    }

    @Test
    void verifyTwoFactor_invalidToken_throws() {
        when(jwtUtil.isTwoFactorToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> authService.verifyTwoFactor(new TwoFactorVerifyRequest("bad", "123456")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void enableTotp_validCode_enablesAndReturnsRecoveryCodes() throws Exception {
        User user = verifiedUser();
        user.setTotpSecret("enc-secret");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(encryptionService.decrypt("enc-secret")).thenReturn("SECRET");
        when(totpService.verify("SECRET", "123456")).thenReturn(true);

        TotpEnableResponse response = authService.enableTotp("alice", "123456");

        assertThat(user.isTotpEnabled()).isTrue();
        assertThat(response.recoveryCodes()).hasSize(10);
        verify(recoveryCodeRepository, times(10)).save(any(RecoveryCode.class));
    }

    @Test
    void login_unverifiedAccount_throwsEmailNotVerifiedAfterPasswordCheck() {
        User user = new User("alice", "alice@example.com", "$hashed$"); // enabled = false
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongP@ss1", "$hashed$")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "StrongP@ss1")))
                .isInstanceOf(EmailNotVerifiedException.class);
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_accountThrottled_throwsTooManyRequestsBeforeDbLookup() {
        when(rateLimitingService.tryConsumeLogin("alice")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "StrongP@ss1")))
                .isInstanceOf(TooManyRequestsException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void login_unknownUsername_throwsInvalidCredentials() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "any")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder).matches(eq("any"), anyString());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = verifiedUser();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$hashed$")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // --- delete account ---

    @Test
    void deleteAccount_correctPassword_purgesAllDataThenUser() {
        User user = verifiedUser();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongP@ss1", "$hashed$")).thenReturn(true);

        MessageResponse response = authService.deleteAccount("alice", "StrongP@ss1");

        assertThat(response.message()).contains("permanently deleted");
        verify(passwordRepository).deleteByUser(user);
        verify(recoveryCodeRepository).deleteByUser(user);
        verify(tokenRepository).deleteByUser(user);
        verify(refreshTokenService).revokeAll(user);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccount_wrongPassword_throwsAndDeletesNothing() {
        User user = verifiedUser();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$hashed$")).thenReturn(false);

        assertThatThrownBy(() -> authService.deleteAccount("alice", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(userRepository, never()).delete(any());
        verify(passwordRepository, never()).deleteByUser(any());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    void login_outdatedHash_triggersRehash() {
        User user = verifiedUser();
        user.setMasterPasswordHash("$bcrypt$");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongP@ss1", "$bcrypt$")).thenReturn(true);
        when(passwordEncoder.upgradeEncoding("$bcrypt$")).thenReturn(true);
        when(passwordEncoder.encode("StrongP@ss1")).thenReturn("$argon2$");
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");

        authService.login(new LoginRequest("alice", "StrongP@ss1"));

        verify(userRepository).save(argThat(u -> u.getMasterPasswordHash().equals("$argon2$")));
    }
}
