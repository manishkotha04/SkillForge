package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.entity.LeaveRequest;
import com.skillforge.skillforge.entity.OwnershipTransfer;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.enums.LeaveRequestStatus;
import com.skillforge.skillforge.enums.OwnershipTransferStatus;
import com.skillforge.skillforge.repository.LeaveRequestRepository;
import com.skillforge.skillforge.repository.OwnershipTransferRepository;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
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

    public NotificationController(
            CurrentUserService currentUserService,
            ProjectInvitationRepository invitationRepository,
            LeaveRequestRepository leaveRequestRepository,
            OwnershipTransferRepository ownershipTransferRepository) {

        this.currentUserService = currentUserService;
        this.invitationRepository = invitationRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.ownershipTransferRepository = ownershipTransferRepository;
    }

    @GetMapping("/notifications")
    public String showNotifications(Model model) {

        AppUser currentUser = currentUserService.getCurrentUser();
        Long userId = currentUser.getId();

        List<ProjectInvitation> invitations =
                invitationRepository.findByInvitedUser_IdAndStatus(
                        userId,
                        InvitationStatus.PENDING);

        List<LeaveRequest> leaveRequests =
                leaveRequestRepository.findByProject_Owner_IdAndStatus(
                        userId,
                        LeaveRequestStatus.PENDING);

        List<OwnershipTransfer> ownershipTransfers =
                ownershipTransferRepository.findByNominatedUser_IdAndStatus(
                        userId,
                        OwnershipTransferStatus.PENDING);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("invitations", invitations);
        model.addAttribute("leaveRequests", leaveRequests);
        model.addAttribute("ownershipTransfers", ownershipTransfers);

        long pendingCount = (long) invitations.size()
                + leaveRequests.size()
                + ownershipTransfers.size();

        model.addAttribute("pendingNotificationCount", pendingCount);

        return "notifications/index";
    }
}
