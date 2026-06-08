package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Parameters for random password generation. Stays a class (not a record) so that
 * {@code length} and {@code includeSymbols} can carry field-level defaults (16 and
 * {@code true} respectively), which records do not support.
 */
@Getter
@Setter
public class PasswordGenerationRequest {
    @Min(8)
    private int length = 16;
    private boolean includeSymbols = true;
}
