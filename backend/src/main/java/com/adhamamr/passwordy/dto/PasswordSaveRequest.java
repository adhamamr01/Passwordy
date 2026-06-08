package com.adhamamr.passwordy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordSaveRequest {
    @NotBlank
    private String label;

    @NotBlank
    private String password;

    private String username;
    private String url;
    private String notes;

    @NotBlank
    private String category;
}
