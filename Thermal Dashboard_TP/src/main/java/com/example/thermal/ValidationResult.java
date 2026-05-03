package com.example.thermal;

public class ValidationResult {
    private final boolean valid;
    private final String message;

    public ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() { return valid; }
    public String getMessage() { return message; }

    public static ValidationResult valid() {
        return new ValidationResult(true, "");
    }

    public static ValidationResult invalid(String msg) {
        return new ValidationResult(false, msg);
    }
}

