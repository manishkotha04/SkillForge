package com.skillforge.skillforge;

import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.LeaveRequestStatus;
import com.skillforge.skillforge.enums.OwnershipTransferStatus;
import com.skillforge.skillforge.enums.ProjectJoinRequestStatus;
import com.skillforge.skillforge.repository.LeaveRequestRepository;
import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.repository.ProjectJoinRequestRepository;
import org.springframework.stereotype.Component;

@Component("navigationInfo")
public class NavigationInfo {

    private final CurrentUserService currentUserService;
    private final ProjectInvitationRepository invitationRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final OwnershipTransferRepository ownershipTransferRepository;
    private final ProjectJoinRequestRepository joinRequestRepository;

    public NavigationInfo(
            CurrentUserService currentUserService,
            ProjectInvitationRepository invitationRepository,
            LeaveRequestRepository leaveRequestRepository,
            OwnershipTransferRepository ownershipTransferRepository,
            ProjectJoinRequestRepository joinRequestRepository) {
        this.currentUserService = currentUserService;
        this.invitationRepository = invitationRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.ownershipTransferRepository = ownershipTransferRepository;
        this.joinRequestRepository = joinRequestRepository;
    }

    public long pendingNotificationCount() {
        Long userId = currentUserService.getCurrentUser().getId();
        long invitations = invitationRepository.countByInvitedUser_IdAndStatus(
                userId, InvitationStatus.PENDING);
        long leaveRequests = leaveRequestRepository.countByProject_Owner_IdAndStatus(
                userId, LeaveRequestStatus.PENDING);
        long ownershipTransfers = ownershipTransferRepository.countByNominatedUser_IdAndStatus(
                userId, OwnershipTransferStatus.PENDING);
        long joinRequests = joinRequestRepository.countByProject_Owner_IdAndStatus(
                userId, ProjectJoinRequestStatus.PENDING);
        return invitations + leaveRequests + ownershipTransfers + joinRequests;
    }
}
