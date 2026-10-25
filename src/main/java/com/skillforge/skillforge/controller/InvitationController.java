
package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;
import com.skillforge.skillforge.service.ProjectService;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InvitationController {

    private final ProjectService projectService;
    private final ProjectInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public InvitationController(
            ProjectService projectService,
            ProjectInvitationRepository invitationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {
        this.projectService = projectService;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

@GetMapping("/invitations")
public String listInvitations(Model model) {
    AppUser currentUser = currentUserService.getCurrentUser();

    List<ProjectInvitation> invitations =
            invitationRepository.findByInvitedUser_IdAndStatus(
                    currentUser.getId(),
                    InvitationStatus.PENDING
            );

    model.addAttribute("invitations", invitations);
    return "invitations/list";
}

    @PostMapping("/projects/{projectId}/invitations")
    public String inviteUser(
            @PathVariable Long projectId,
            @RequestParam String username,
            RedirectAttributes redirectAttributes) {
        try {
            AppUser invitedUser = userRepository
                    .findByUsernameIgnoreCase(username.trim())
                    .orElseThrow(() -> new NoSuchElementException(
                            "No user found with that username."));

            projectService.inviteUser(projectId, invitedUser.getId());

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Invitation sent successfully.");
        } catch (IllegalArgumentException | NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        } catch (AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        }

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public String acceptInvitation(
            @PathVariable Long invitationId,
            RedirectAttributes redirectAttributes) {
        try {
            projectService.acceptInvitation(invitationId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Invitation accepted.");
        } catch (IllegalArgumentException | NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        } catch (AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        }

        return "redirect:/invitations";
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public String rejectInvitation(
            @PathVariable Long invitationId,
            RedirectAttributes redirectAttributes) {
        try {
            projectService.rejectInvitation(invitationId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Invitation rejected.");
        } catch (IllegalArgumentException | NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        } catch (AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        }

        return "redirect:/invitations";
    }
}
