package com.adhamamr.passwordy.controller;

import com.adhamamr.passwordy.dto.PasswordGenerationRequest;
import com.adhamamr.passwordy.dto.PasswordResponse;
import com.adhamamr.passwordy.dto.PasswordSaveRequest;
import com.adhamamr.passwordy.dto.PinGenerationRequest;
import com.adhamamr.passwordy.service.PasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for password generation and per-user password CRUD.
 *
 * <p>Generation routes are public; the {@code /passwords} routes require authentication and
 * operate on the caller's own records only. The authenticated username is read from the
 * security context and passed to the service, which enforces ownership. This controller
 * delegates all logic (including decryption) to {@link PasswordService} and holds no data
 * access of its own.
 */
@RestController
@RequestMapping("/api")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    private String getAuthenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("/password/generate")
    public Map<String, String> generatePassword(@RequestBody PasswordGenerationRequest request) {
        String password = passwordService.generatePassword(request.getLength(), request.isIncludeSymbols());
        return Map.of("password", password);
    }

    @PostMapping("/password/generate-pin")
    public Map<String, String> generatePin(@RequestBody PinGenerationRequest request) {
        return Map.of("pin", passwordService.generatePin(request.getLength()));
    }

    @GetMapping("/password/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(List.of(
                "Social Media", "Banking", "Email", "Work", "Shopping", "Entertainment", "Other"
        ));
    }

    @PostMapping("/passwords")
    public ResponseEntity<PasswordResponse> savePassword(@RequestBody PasswordSaveRequest request) {
        PasswordResponse response = passwordService.savePassword(request, getAuthenticatedUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/passwords")
    public ResponseEntity<List<PasswordResponse>> getAllPasswords() {
        return ResponseEntity.ok(passwordService.getAllPasswords(getAuthenticatedUsername()));
    }

    @GetMapping("/passwords/{id}")
    public ResponseEntity<PasswordResponse> getPasswordById(@PathVariable Long id) {
        return ResponseEntity.ok(passwordService.getPasswordById(id, getAuthenticatedUsername()));
    }

    @PutMapping("/passwords/{id}")
    public ResponseEntity<PasswordResponse> updatePassword(@PathVariable Long id,
                                                           @RequestBody PasswordSaveRequest request) {
        return ResponseEntity.ok(passwordService.updatePassword(id, request, getAuthenticatedUsername()));
    }

    @DeleteMapping("/passwords/{id}")
    public ResponseEntity<Void> deletePassword(@PathVariable Long id) {
        passwordService.deletePassword(id, getAuthenticatedUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/passwords/{id}/decrypt")
    public ResponseEntity<Map<String, String>> decryptPassword(@PathVariable Long id) {
        String decrypted = passwordService.decryptPassword(id, getAuthenticatedUsername());
        return ResponseEntity.ok(Map.of("password", decrypted));
    }
}
