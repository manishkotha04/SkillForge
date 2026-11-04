package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.ProjectUpdateRequest;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.service.ProjectManagementService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class ProjectManagementController {

    private final ProjectManagementService projectManagementService;

    public ProjectManagementController(ProjectManagementService projectManagementService) {
        this.projectManagementService = projectManagementService;
    }

    @PostMapping("/projects/create-with-deadline")
    public String createProject(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String requiredSkillsText,
            @RequestParam(required = false) Integer maxTeamSize,
            @RequestParam(required = false) LocalDate deadline,
            RedirectAttributes redirectAttributes) {
        try {
            Project project = projectManagementService.createProjectWithDeadline(
                    name, description, requiredSkillsText, maxTeamSize, deadline);
            redirectAttributes.addFlashAttribute("successMessage", "Project created successfully.");
            return "redirect:/projects/" + project.getId();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/projects/new";
        }
    }

    @GetMapping("/projects/{projectId}/edit")
    public String showEditProjectForm(@PathVariable Long projectId, Model model) {
        Project project = projectManagementService.getProjectForEditing(projectId);
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName(project.getName());
        request.setDescription(project.getDescription());
        request.setRequiredSkillsText(project.getRequiredSkillsText());
        request.setMaxTeamSize(project.getMaxTeamSize());
        request.setDeadline(project.getDeadline());

        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("projectUpdateRequest", request);
        return "projects/edit";
    }

    @PostMapping("/projects/{projectId}/edit")
    public String updateProject(
            @PathVariable Long projectId,
            @Valid @ModelAttribute("projectUpdateRequest") ProjectUpdateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Project project = projectManagementService.getProjectForEditing(projectId);
        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);

        if (bindingResult.hasErrors()) {
            return "projects/edit";
        }

        try {
            projectManagementService.updateProject(projectId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Project updated successfully.");
            return "redirect:/projects/" + projectId;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "projects/edit";
        }
    }

    @PostMapping("/projects/{projectId}/leaders/{userId}")
    public String appointLeader(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {
        try {
            projectManagementService.appointLeader(projectId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Project leader appointed successfully.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/projects/{projectId}/leaders/{userId}/remove")
    public String removeLeader(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {
        try {
            projectManagementService.removeLeader(projectId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Project leader role removed. The student remains a project member.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/projects/" + projectId;
    }

}
