package com.adhamamr.passwordy.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MasterPasswordValidatorTest {

    @Test
    void validPassword_passes() {
        var result = MasterPasswordValidator.validate("StrongP@ss1");
        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void nullPassword_fails() {
        var result = MasterPasswordValidator.validate(null);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).contains("Password cannot be empty");
    }

    @Test
    void blankPassword_fails() {
        var result = MasterPasswordValidator.validate("   ");
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).contains("Password cannot be empty");
    }

    @Test
    void tooShort_fails() {
        var result = MasterPasswordValidator.validate("Ab1!");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("at least 8");
    }

    @Test
    void missingUppercase_fails() {
        var result = MasterPasswordValidator.validate("strongp@ss1");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("uppercase");
    }

    @Test
    void missingLowercase_fails() {
        var result = MasterPasswordValidator.validate("STRONGP@SS1");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("lowercase");
    }

    @Test
    void missingDigit_fails() {
        var result = MasterPasswordValidator.validate("StrongP@ss");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("number");
    }

    @Test
    void missingSpecial_fails() {
        var result = MasterPasswordValidator.validate("StrongPass1");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("special character");
    }

    @Test
    void multipleViolations_reportsAll() {
        var result = MasterPasswordValidator.validate("weak");
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().size()).isGreaterThan(1);
    }
}
