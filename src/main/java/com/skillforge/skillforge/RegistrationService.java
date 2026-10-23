
package com.skillforge.skillforge;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegistrationForm form) {
        String fullName = form.getFullName().trim();
        String email = form.getEmail().trim().toLowerCase();
        String username = form.getUsername().trim();
        String password = form.getPassword();

        if (fullName.isEmpty()
                || email.isEmpty()
                || username.length() < 3
                || password == null
                || password.length() < 8
                || !email.contains("@")) {
            throw new IllegalArgumentException(
                    "Please enter valid details. Username must contain at least 3 characters and password at least 8 characters.");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException(
                    "That username is already taken.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "An account with that email already exists.");
        }

        AppUser user = new AppUser();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));

        userRepository.save(user);
    }
}
