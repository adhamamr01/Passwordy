package com.adhamamr.passwordy.controller;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.RegisterRequest;
import com.adhamamr.passwordy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints: register and login. Both return an {@link AuthResponse}
 * containing a freshly issued JWT the client uses for all subsequent protected calls.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}