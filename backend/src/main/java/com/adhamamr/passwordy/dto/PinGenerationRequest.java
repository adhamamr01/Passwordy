package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Parameters for numeric PIN generation. Stays a class (not a record) so that
 * {@code length} can carry a field-level default of 6. Valid range is 4–12 digits.
 */
@Getter
@Setter
public class PinGenerationRequest {
    @Min(4)
    @Max(12)
    private int length = 6;
}
