package com.adhamamr.passwordy.controller;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.ForgotPasswordRequest;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.dto.RegisterRequest;
import com.adhamamr.passwordy.dto.ResetPasswordRequest;
import com.adhamamr.passwordy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints.
 *
 * <p>{@code register} returns a generic {@link MessageResponse} (HTTP 202) and no token — the
 * account must be verified via the emailed link ({@code verify}) before {@code login} will issue
 * a JWT. The generic register response is deliberate: it never reveals whether an account exists.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.register(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<MessageResponse> verify(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verify(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.accepted().body(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.accepted().body(authService.resendVerification(request));
    }
}
