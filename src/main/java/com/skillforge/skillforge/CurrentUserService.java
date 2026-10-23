
package com.skillforge.skillforge;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException(
                    "Authentication is required.");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            throw new AccessDeniedException(
                    "Unable to identify the authenticated user.");
        }

        return userRepository
                .findByUsernameIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new AccessDeniedException(
                        "The authenticated user could not be found."));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
