package com.skillforge.skillforge;

import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.LeaveRequestStatus;
import com.skillforge.skillforge.enums.OwnershipTransferStatus;
import com.skillforge.skillforge.repository.LeaveRequestRepository;
import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import org.springframework.stereotype.Component;

/**
 * Supplies the number of pending items shown in the shared navigation.
 * Counts are calculated from the existing invitation, leave-request and
 * ownership-transfer records; no separate notification table is required.
 */
@Component("navigationInfo")
public class NavigationInfo {

    private final CurrentUserService currentUserService;
    private final ProjectInvitationRepository invitationRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final OwnershipTransferRepository ownershipTransferRepository;

    public NavigationInfo(
            CurrentUserService currentUserService,
            ProjectInvitationRepository invitationRepository,
            LeaveRequestRepository leaveRequestRepository,
            OwnershipTransferRepository ownershipTransferRepository) {

        this.currentUserService = currentUserService;
        this.invitationRepository = invitationRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.ownershipTransferRepository = ownershipTransferRepository;
    }

    public long pendingNotificationCount() {

        AppUser currentUser = currentUserService.getCurrentUser();
        Long userId = currentUser.getId();

        long invitations =
                invitationRepository.countByInvitedUser_IdAndStatus(
                        userId,
                        InvitationStatus.PENDING);

        long leaveRequests =
                leaveRequestRepository.countByProject_Owner_IdAndStatus(
                        userId,
                        LeaveRequestStatus.PENDING);

        long ownershipTransfers =
                ownershipTransferRepository.countByNominatedUser_IdAndStatus(
                        userId,
                        OwnershipTransferStatus.PENDING);

        return invitations + leaveRequests + ownershipTransfers;
    }
}
