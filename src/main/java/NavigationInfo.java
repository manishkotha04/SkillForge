package com.skillforge.skillforge;

import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;

import org.springframework.stereotype.Component;

/**
 * Read-only helper used by the shared navigation fragment.
 *
 * Provides the number of pending project invitations for the
 * currently authenticated user.
 */
@Component("navigationInfo")
public class NavigationInfo {

    private final CurrentUserService currentUserService;
    private final ProjectInvitationRepository invitationRepository;

    public NavigationInfo(
            CurrentUserService currentUserService,
            ProjectInvitationRepository invitationRepository) {

        this.currentUserService = currentUserService;
        this.invitationRepository = invitationRepository;
    }

    public long pendingInvitationCount() {

        AppUser currentUser = currentUserService.getCurrentUser();

        if (currentUser == null) {
            return 0;
        }

        return invitationRepository.countByInvitedUser_IdAndStatus(
                currentUser.getId(),
                InvitationStatus.PENDING);
    }
}