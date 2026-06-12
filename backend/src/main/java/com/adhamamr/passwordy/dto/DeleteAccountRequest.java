package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirms account deletion by re-supplying the current master password. */
public record DeleteAccountRequest(@NotBlank String masterPassword) {
}
