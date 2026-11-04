
package com.skillforge.skillforge;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void initializeValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository,
                passwordEncoder,
                validator
        );
    }

    private RegistrationForm validForm() {

        RegistrationForm form = new RegistrationForm();

        form.setFullName("Manish Kotha");
        form.setEmail("manish@example.com");
        form.setUsername("ManishUser");
        form.setPassword("StrongPassword123");

        return form;
    }

    @Test
    void registerCreatesUserWithNormalizedDetailsAndEncodedPassword() {

        RegistrationForm form = validForm();

        form.setFullName("  Manish Kotha  ");
        form.setEmail("  MANISH@EXAMPLE.COM  ");
        form.setUsername("  ManishUser  ");

        when(userRepository.existsByUsernameIgnoreCase("ManishUser"))
                .thenReturn(false);

        when(userRepository.existsByEmailIgnoreCase("manish@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("StrongPassword123"))
                .thenReturn("encoded-password");

        registrationService.register(form);

        ArgumentCaptor<AppUser> captor =
                ArgumentCaptor.forClass(AppUser.class);

        verify(userRepository).save(captor.capture());

        AppUser savedUser = captor.getValue();

        assertEquals("Manish Kotha", savedUser.getFullName());
        assertEquals("manish@example.com", savedUser.getEmail());
        assertEquals("ManishUser", savedUser.getUsername());

        assertEquals("encoded-password", savedUser.getPasswordHash());

        assertNotEquals(
                "StrongPassword123",
                savedUser.getPasswordHash()
        );

        verify(passwordEncoder).encode("StrongPassword123");
    }

    @Test
    void registerAcceptsValidEmailFromAnyDomain() {

        RegistrationForm form = validForm();
        form.setEmail("student@company.org");

        when(userRepository.existsByUsernameIgnoreCase("ManishUser"))
                .thenReturn(false);

        when(userRepository.existsByEmailIgnoreCase("student@company.org"))
                .thenReturn(false);

        when(passwordEncoder.encode("StrongPassword123"))
                .thenReturn("encoded-password");

        registrationService.register(form);

        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    void registerRejectsNullForm() {

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(null)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsMissingRequiredFields() {

        RegistrationForm form = new RegistrationForm();

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(form)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsInvalidEmail() {

        RegistrationForm form = validForm();
        form.setEmail("not-an-email");

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(form)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsBlankFullName() {

        RegistrationForm form = validForm();
        form.setFullName("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(form)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsUsernameShorterThanThreeCharacters() {

        RegistrationForm form = validForm();
        form.setUsername("ab");

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(form)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsPasswordShorterThanEightCharacters() {

        RegistrationForm form = validForm();
        form.setPassword("short");

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(form)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsDuplicateUsername() {

        RegistrationForm form = validForm();

        when(userRepository.existsByUsernameIgnoreCase("ManishUser"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(form)
        );

        verify(userRepository)
                .existsByUsernameIgnoreCase("ManishUser");

        verify(userRepository, never())
                .save(any(AppUser.class));

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsDuplicateEmail() {

        RegistrationForm form = validForm();

        when(userRepository.existsByUsernameIgnoreCase("ManishUser"))
                .thenReturn(false);

        when(userRepository.existsByEmailIgnoreCase("manish@example.com"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(form)
        );

        verify(userRepository)
                .existsByEmailIgnoreCase("manish@example.com");

        verify(userRepository, never())
                .save(any(AppUser.class));

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void registerRejectsUsernameDuplicateIgnoringCase() {

        RegistrationForm form = validForm();
        form.setUsername("MANISHUSER");

        when(userRepository.existsByUsernameIgnoreCase("MANISHUSER"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(form)
        );

        verify(userRepository, never())
                .save(any(AppUser.class));
    }

}
