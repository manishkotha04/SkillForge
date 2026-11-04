package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.service.ProjectDiscoveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProjectDiscoveryController {

    private final ProjectDiscoveryService discoveryService;

    public ProjectDiscoveryController(ProjectDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/explore/projects")
    public String exploreProjects(
            @RequestParam(required = false) String q,
            Model model) {
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("projects", discoveryService.findAvailableProjects(q));
        return "projects/explore";
    }

    @PostMapping("/explore/projects/{projectId}/join")
    public String requestToJoin(
            @PathVariable Long projectId,
            @RequestParam(required = false) String q,
            RedirectAttributes redirectAttributes) {
        try {
            discoveryService.requestToJoin(projectId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Your join request has been sent to the project owner.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/explore/projects" + (q == null || q.isBlank() ? "" : "?q=" + q);
    }

    @PostMapping("/project-join-requests/{requestId}/approve")
    public String approveJoinRequest(
            @PathVariable Long requestId,
            RedirectAttributes redirectAttributes) {
        try {
            discoveryService.approveJoinRequest(requestId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Join request approved. The student has been added to the project.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/project-join-requests/{requestId}/reject")
    public String rejectJoinRequest(
            @PathVariable Long requestId,
            RedirectAttributes redirectAttributes) {
        try {
            discoveryService.rejectJoinRequest(requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Join request rejected.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/notifications";
    }
}
