package com.machly.app.domain.registration;

import java.util.regex.Pattern;

public class UserRegistrationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final EmailRegistrationChecker emailRegistrationChecker;

    public UserRegistrationService(EmailRegistrationChecker emailRegistrationChecker) {
        this.emailRegistrationChecker = emailRegistrationChecker;
    }

    public RegistrationValidationResult validateNewUser(String name, String email, String password) {
        if (name == null || name.trim().isEmpty()) {
            return RegistrationValidationResult.invalid("El nombre es obligatorio.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return RegistrationValidationResult.invalid("Ingresa un correo electrónico válido.");
        }
        if (password == null || password.length() < 8) {
            return RegistrationValidationResult.invalid("La contraseña debe tener al menos 8 caracteres.");
        }
        if (emailRegistrationChecker.isEmailRegistered(email.trim())) {
            return RegistrationValidationResult.invalid("El correo electrónico ya está registrado.");
        }
        return RegistrationValidationResult.valid();
    }
}
