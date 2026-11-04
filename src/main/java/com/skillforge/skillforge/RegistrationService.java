
package com.skillforge.skillforge;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Validator validator) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional
    public void register(RegistrationForm form) {

        if (form == null) {
            throw new IllegalArgumentException(
                    "Registration information is required.");
        }

        // Normalize user-entered text before validation.
        form.setFullName(trimToNull(form.getFullName()));
        form.setEmail(trimToNull(form.getEmail()));
        form.setUsername(trimToNull(form.getUsername()));

        // Validate fields on the server.
        Set<ConstraintViolation<RegistrationForm>> violations =
                validator.validate(form);

        if (!violations.isEmpty()) {

            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(" "));

            throw new IllegalArgumentException(errorMessage);
        }

        String fullName = form.getFullName();
        String email = form.getEmail().toLowerCase(Locale.ROOT);
        String username = form.getUsername();
        String password = form.getPassword();

        // Check case-insensitive username uniqueness.
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException(
                    "That username is already taken.");
        }

        // Check case-insensitive email uniqueness.
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "An account with that email already exists.");
        }

        // Create the user only after validation succeeds.
        AppUser user = new AppUser();

        user.setFullName(fullName);
        user.setEmail(email);
        user.setUsername(username);

        // Never store the raw password.
        user.setPasswordHash(passwordEncoder.encode(password));

        userRepository.save(user);
    }

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
