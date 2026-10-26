package com.machly.app.domain.registration;

public final class RegistrationValidationResult {
    private final boolean valid;
    private final String message;

    private RegistrationValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static RegistrationValidationResult valid() {
        return new RegistrationValidationResult(true, "");
    }

    public static RegistrationValidationResult invalid(String message) {
        return new RegistrationValidationResult(false, message);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}
