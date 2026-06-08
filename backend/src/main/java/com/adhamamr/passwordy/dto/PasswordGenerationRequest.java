package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordGenerationRequest {
    @Min(8)
    private int length = 16;
    private boolean includeSymbols = true;
}
