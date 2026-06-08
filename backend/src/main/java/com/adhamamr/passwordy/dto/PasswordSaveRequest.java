package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordSaveRequest(
        @NotBlank String label,
        @NotBlank String password,
        String username,
        String url,
        String notes,
        @NotBlank String category) {
}
