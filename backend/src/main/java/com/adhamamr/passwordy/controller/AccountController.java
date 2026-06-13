package com.adhamamr.passwordy.controller;

import com.adhamamr.passwordy.dto.AccountExportResponse;
import com.adhamamr.passwordy.dto.DeleteAccountRequest;
import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Authenticated account self-management for the signed-in user (the username is taken from the
 * security context). Deleting the account permanently removes it and all data it owns after the
 * master password is re-confirmed.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        return ResponseEntity.ok(authService.deleteAccount(currentUsername(), request.masterPassword()));
    }

    /** GDPR data export: the user's profile plus all vault entries (decrypted) as JSON. */
    @GetMapping("/export")
    public ResponseEntity<AccountExportResponse> exportData() {
        return ResponseEntity.ok(authService.exportAccount(currentUsername()));
    }
}
