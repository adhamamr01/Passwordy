package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String masterPassword) {
}
