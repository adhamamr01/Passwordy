package com.adhamamr.passwordy.dto;

public record AuthResponse(String token, String username, String email, String message) {
}
