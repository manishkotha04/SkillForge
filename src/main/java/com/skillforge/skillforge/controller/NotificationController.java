package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.entity.LeaveRequest;
import com.skillforge.skillforge.entity.OwnershipTransfer;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.entity.ProjectJoinRequest;
import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.LeaveRequestStatus;
import com.skillforge.skillforge.enums.OwnershipTransferStatus;
import com.skillforge.skillforge.enums.ProjectJoinRequestStatus;
import com.skillforge.skillforge.repository.LeaveRequestRepository;
import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.repository.ProjectJoinRequestRepository;
import com.skillforge.skillforge.service.ProjectDiscoveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class NotificationController {

    private final CurrentUserService currentUserService;
    private final ProjectInvitationRepository invitationRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final OwnershipTransferRepository ownershipTransferRepository;
    private final ProjectJoinRequestRepository joinRequestRepository;
    private final ProjectDiscoveryService projectDiscoveryService;

    public NotificationController(
            CurrentUserService currentUserService,
            ProjectInvitationRepository invitationRepository,
            LeaveRequestRepository leaveRequestRepository,
            OwnershipTransferRepository ownershipTransferRepository,
            ProjectJoinRequestRepository joinRequestRepository,
            ProjectDiscoveryService projectDiscoveryService) {
        this.currentUserService = currentUserService;
        this.invitationRepository = invitationRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.ownershipTransferRepository = ownershipTransferRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.projectDiscoveryService = projectDiscoveryService;
    }

    @GetMapping("/notifications")
    public String showNotifications(Model model) {
        AppUser currentUser = currentUserService.getCurrentUser();
        Long userId = currentUser.getId();

        // Clean up legacy duplicate pending records before displaying inbox items.
        projectDiscoveryService.reconcileExistingMembershipsForCurrentUser();

        List<ProjectInvitation> invitations = invitationRepository
                .findByInvitedUser_IdAndStatus(userId, InvitationStatus.PENDING);
        List<LeaveRequest> leaveRequests = leaveRequestRepository
                .findByProject_Owner_IdAndStatus(userId, LeaveRequestStatus.PENDING);
        List<OwnershipTransfer> ownershipTransfers = ownershipTransferRepository
                .findByNominatedUser_IdAndStatus(userId, OwnershipTransferStatus.PENDING);
        List<ProjectJoinRequest> receivedJoinRequests = joinRequestRepository
                .findByProject_Owner_IdAndStatusOrderByCreatedAtDesc(
                        userId, ProjectJoinRequestStatus.PENDING);
        List<ProjectJoinRequest> myJoinRequests = joinRequestRepository
                .findByRequestedBy_IdOrderByCreatedAtDesc(userId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("invitations", invitations);
        model.addAttribute("leaveRequests", leaveRequests);
        model.addAttribute("ownershipTransfers", ownershipTransfers);
        model.addAttribute("receivedJoinRequests", receivedJoinRequests);
        model.addAttribute("myJoinRequests", myJoinRequests);

        long pendingCount = (long) invitations.size() + leaveRequests.size()
                + ownershipTransfers.size() + receivedJoinRequests.size();
        model.addAttribute("pendingNotificationCount", pendingCount);
        return "notifications/index";
    }
}
