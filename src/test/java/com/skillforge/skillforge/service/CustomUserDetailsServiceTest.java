
package com.skillforge.skillforge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsernameReturnsCorrectUserDetails() {

        AppUser appUser = mock(AppUser.class);

        when(appUser.getUsername()).thenReturn("manish");
        when(appUser.getPasswordHash()).thenReturn("encoded-password");

        when(userRepository.findByUsernameIgnoreCase("manish"))
                .thenReturn(Optional.of(appUser));

        UserDetails result =
                userDetailsService.loadUserByUsername("manish");

        assertEquals("manish", result.getUsername());
        assertEquals("encoded-password", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_USER")));

        verify(userRepository)
                .findByUsernameIgnoreCase("manish");
    }

    @Test
    void loadUserByUsernameUsesCaseInsensitiveRepositoryLookup() {

        AppUser appUser = mock(AppUser.class);

        when(appUser.getUsername()).thenReturn("Manish");
        when(appUser.getPasswordHash()).thenReturn("encoded-password");

        when(userRepository.findByUsernameIgnoreCase("MANISH"))
                .thenReturn(Optional.of(appUser));

        UserDetails result =
                userDetailsService.loadUserByUsername("MANISH");

        assertEquals("Manish", result.getUsername());

        verify(userRepository)
                .findByUsernameIgnoreCase("MANISH");
    }

    @Test
    void loadUserByUsernameThrowsExceptionForUnknownUser() {

        when(userRepository.findByUsernameIgnoreCase("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown")
        );

        verify(userRepository)
                .findByUsernameIgnoreCase("unknown");
    }
}
