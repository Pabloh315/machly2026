package com.machly.app.domain.registration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserRegistrationServiceTest {

    @Test
    public void acceptsRegistrationWhenDataIsValidAndEmailIsAvailable() {
        UserRegistrationService service = new UserRegistrationService(email -> false);
        RegistrationValidationResult result = service.validateNewUser(
                "Ana Pérez", "ana.perez@example.com", "claveSegura8"
        );
        assertTrue(result.isValid());
    }

    @Test
    public void rejectsRegistrationWhenEmailAlreadyExists() {
        UserRegistrationService service = new UserRegistrationService(
                email -> email.equals("ana.perez@example.com")
        );
        RegistrationValidationResult result = service.validateNewUser(
                "Ana Pérez", "ana.perez@example.com", "claveSegura8"
        );
        assertFalse(result.isValid());
    }
}
