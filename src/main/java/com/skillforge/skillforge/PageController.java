
package com.skillforge.skillforge;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {

    private final RegistrationService registrationService;

    public PageController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping({"/", "/login"})
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
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
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
