package com.adhamamr.passwordy.controller;

import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.dto.TotpCodeRequest;
import com.adhamamr.passwordy.dto.TotpEnableResponse;
import com.adhamamr.passwordy.dto.TotpSetupResponse;
import com.adhamamr.passwordy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Authenticated TOTP two-factor management for the signed-in user (the username is taken from
 * the security context). The login-time verification step lives on the public
 * {@code /api/auth/2fa/verify} route instead.
 */
@RestController
@RequestMapping("/api/account/2fa")
public class TwoFactorController {

    private final AuthService authService;

    public TwoFactorController(AuthService authService) {
        this.authService = authService;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("/setup")
    public ResponseEntity<TotpSetupResponse> setup() {
        return ResponseEntity.ok(authService.setupTotp(currentUsername()));
    }

    @PostMapping("/enable")
    public ResponseEntity<TotpEnableResponse> enable(@Valid @RequestBody TotpCodeRequest request) {
        return ResponseEntity.ok(authService.enableTotp(currentUsername(), request.code()));
    }

    @PostMapping("/disable")
    public ResponseEntity<MessageResponse> disable(@Valid @RequestBody TotpCodeRequest request) {
        return ResponseEntity.ok(authService.disableTotp(currentUsername(), request.code()));
    }
}
