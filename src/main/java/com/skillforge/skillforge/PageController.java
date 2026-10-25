package com.skillforge.skillforge;

import com.skillforge.skillforge.enums.InvitationStatus;
import com.skillforge.skillforge.repository.ProjectInvitationRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {

    private final RegistrationService registrationService;
    private final CurrentUserService currentUserService;
    private final ProjectInvitationRepository invitationRepository;

    public PageController(
            RegistrationService registrationService,
            CurrentUserService currentUserService,
            ProjectInvitationRepository invitationRepository) {

        this.registrationService = registrationService;
        this.currentUserService = currentUserService;
        this.invitationRepository = invitationRepository;
    }

    @GetMapping({"/", "/login"})
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute(
                    "registrationForm",
                    new RegistrationForm());
        }

        return "auth/register";
    }

    @PostMapping("/register")
    public String submitRegistration(
            @ModelAttribute("registrationForm") RegistrationForm form,
            Model model) {

        try {
            registrationService.register(form);
            return "redirect:/login?registered";

        } catch (IllegalArgumentException ex) {
            model.addAttribute(
                    "errorMessage",
                    ex.getMessage());

            return "auth/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        AppUser currentUser = currentUserService.getCurrentUser();

        long pendingInvitationCount =
                invitationRepository.countByInvitedUser_IdAndStatus(
                        currentUser.getId(),
                        InvitationStatus.PENDING);

        model.addAttribute(
                "pendingInvitationCount",
                pendingInvitationCount);

        // ADDED: used only for the personalised greeting
        model.addAttribute("currentUser", currentUser);

        return "dashboard";
    }
}