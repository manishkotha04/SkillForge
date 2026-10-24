
package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.service.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public String listProjects(Model model) {
        model.addAttribute("projects", projectService.getMyProjects());
        return "projects/list";
    }

    @GetMapping("/projects/new")
    public String showCreateProjectForm() {
        return "projects/create";
    }

    @PostMapping("/projects")
    public String createProject(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String requiredSkillsText,
            @RequestParam(required = false) Integer maxTeamSize,
            RedirectAttributes redirectAttributes) {

        try {
               projectService.createProject(
                  name, description, requiredSkillsText, maxTeamSize);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Project created successfully.");

            return "redirect:/projects";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
            return "redirect:/projects/new";
        }
    }
}
