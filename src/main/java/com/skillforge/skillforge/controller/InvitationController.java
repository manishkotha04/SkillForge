package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.UserRepository;
import com.skillforge.skillforge.entity.ProjectInvitation;
import com.skillforge.skillforge.service.ProjectService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
public class InvitationController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    public InvitationController(
            ProjectService projectService,
            UserRepository userRepository) {

        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    /**
     * Backward-compatible route. Invitations are now included in
     * the central Notifications page.
     */
    @GetMapping("/invitations")
    public String listInvitations() {
        return "redirect:/notifications";
    }

    @PostMapping("/projects/{projectId}/invitations")
    public String inviteUser(
            @PathVariable Long projectId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long invitedUserId,
            RedirectAttributes redirectAttributes) {

        try {
            Long targetUserId;

            if (invitedUserId != null) {
                targetUserId = invitedUserId;

            } else if (username != null && !username.trim().isEmpty()) {
                AppUser invitedUser = userRepository
                        .findByUsernameIgnoreCase(username.trim())
                        .orElseThrow(() -> new NoSuchElementException(
                                "No user found with that username."));

                targetUserId = invitedUser.getId();

            } else {
                throw new IllegalArgumentException(
                        "Please select a student to invite.");
            }

            projectService.inviteUser(projectId, targetUserId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Invitation sent successfully.");

        } catch (IllegalArgumentException | NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

        } catch (AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/projects/{projectId}/invitations/{userId}")
    public String inviteStudent(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {

        try {
            projectService.inviteUser(projectId, userId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Invitation sent successfully.");

        } catch (IllegalArgumentException | NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

        } catch (AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/students/search?projectId=" + projectId;
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public String acceptInvitation(
            @PathVariable Long invitationId,
            RedirectAttributes redirectAttributes) {

        try {
            projectService.acceptInvitation(invitationId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Invitation accepted.");

        } catch (IllegalArgumentException | NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

        } catch (AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/notifications";
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public String rejectInvitation(
            @PathVariable Long invitationId,
            RedirectAttributes redirectAttributes) {

        try {
            projectService.rejectInvitation(invitationId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Invitation rejected.");

        } catch (IllegalArgumentException | NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

        } catch (AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/notifications";
    }
}
