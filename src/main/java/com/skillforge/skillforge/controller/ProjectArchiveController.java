package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.service.ProjectArchiveService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProjectArchiveController {

    private final ProjectArchiveService projectArchiveService;

    public ProjectArchiveController(ProjectArchiveService projectArchiveService) {
        this.projectArchiveService = projectArchiveService;
    }

    @PostMapping("/projects/{projectId}/archive")
    public String archiveProject(@PathVariable Long projectId,
                                 RedirectAttributes redirectAttributes) {
        try {
            projectArchiveService.archiveProject(projectId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Project archived successfully. Its records and history have been preserved.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/projects/" + projectId;
    }
}
