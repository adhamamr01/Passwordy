package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PinGenerationRequest {
    @Min(4)
    @Max(12)
    private int length = 6;
}
