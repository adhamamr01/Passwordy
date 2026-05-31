package com.adhamamr.passwordy.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Enforces master-password strength at registration: minimum 8 characters with at least one
 * uppercase letter, lowercase letter, digit, and special character. Returns a
 * {@link ValidationResult} accumulating every failed rule rather than failing on the first,
 * so the caller can report all problems at once. The character-class patterns are compiled
 * once as static constants.
 */
public class MasterPasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern HAS_UPPER   = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER   = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT   = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    public static ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty");
            return new ValidationResult(false, errors);
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (!HAS_UPPER.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        if (!HAS_LOWER.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        if (!HAS_DIGIT.matcher(password).find()) {
            errors.add("Password must contain at least one number");
        }
        if (!HAS_SPECIAL.matcher(password).find()) {
            errors.add("Password must contain at least one special character");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> errors;

        public ValidationResult(boolean isValid, List<String> errors) {
            this.isValid = isValid;
            this.errors = errors;
        }

        public boolean isValid() { return isValid; }
        public List<String> getErrors() { return errors; }
        public String getErrorMessage() { return String.join(", ", errors); }
    }
}
