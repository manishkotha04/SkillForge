
package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.service.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.skillforge.skillforge.entity.Project;
import org.springframework.web.bind.annotation.PathVariable;
import com.skillforge.skillforge.entity.OwnershipTransfer;
import java.util.List;
import com.skillforge.skillforge.entity.LeaveRequest;
import java.util.List;

@Controller
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }


@GetMapping("/projects")
public String listProjects(Model model) {
    model.addAttribute("projects", projectService.getMyProjects());
    model.addAttribute(
            "teamSummaries",
            projectService.getMyProjectTeamSummaries()
    );
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


@GetMapping("/projects/{projectId}")
public String showProjectDetails(@PathVariable Long projectId, Model model) {

    Project project = projectService.getProjectForCurrentUser(projectId);

    model.addAttribute("project", project);

    boolean isProjectOwner = projectService.isProjectOwner(projectId);
    model.addAttribute("isProjectOwner", isProjectOwner);

    if (isProjectOwner) {
        model.addAttribute("projectMembers",
                projectService.getProjectMembers(projectId));
    } else {
        model.addAttribute("hasPendingLeaveRequest",
                projectService.hasPendingLeaveRequest(projectId));
    }

    return "projects/details";
}

@PostMapping("/projects/{projectId}/ownership-transfer")
public String requestOwnershipTransfer(
        @PathVariable Long projectId,
        @RequestParam Long nominatedUserId,
        RedirectAttributes redirectAttributes) {

    try {
        projectService.requestOwnershipTransfer(
                projectId, nominatedUserId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Ownership transfer request sent successfully.");

    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
    }

    return "redirect:/projects/" + projectId;
}


@GetMapping("/ownership-transfers")
public String showOwnershipTransferRequests(Model model) {
    model.addAttribute(
            "transfers",
            projectService.getMyPendingOwnershipTransfers());

    return "projects/ownership-transfers";
}


@PostMapping("/ownership-transfers/{transferId}/accept")
public String acceptOwnershipTransfer(
        @PathVariable Long transferId,
        RedirectAttributes redirectAttributes) {

    try {
        projectService.acceptOwnershipTransfer(transferId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Project ownership transferred successfully.");

    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
    }

    return "redirect:/ownership-transfers";
}


@PostMapping("/ownership-transfers/{transferId}/reject")
public String rejectOwnershipTransfer(
        @PathVariable Long transferId,
        RedirectAttributes redirectAttributes) {

    try {
        projectService.rejectOwnershipTransfer(transferId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Ownership transfer request rejected.");

    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
    }

    return "redirect:/ownership-transfers";
}

@PostMapping("/projects/{projectId}/leave-request")
public String requestToLeaveProject(
        @PathVariable Long projectId,
        RedirectAttributes redirectAttributes) {

    try {
        projectService.requestToLeaveProject(projectId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your leave request has been submitted successfully.");

    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
    }

    return "redirect:/projects/" + projectId;
}

@GetMapping("/leave-requests")
public String showPendingLeaveRequests(Model model) {

    model.addAttribute(
            "leaveRequests",
            projectService.getPendingLeaveRequestsForOwner());

    return "projects/leave-requests";
}

@PostMapping("/leave-requests/{requestId}/approve")
public String approveLeaveRequest(
        @PathVariable Long requestId,
        RedirectAttributes redirectAttributes) {

    try {
        projectService.approveLeaveRequest(requestId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Leave request approved. The member has been removed.");

    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
    }

    return "redirect:/leave-requests";
}

@PostMapping("/leave-requests/{requestId}/reject")
public String rejectLeaveRequest(
        @PathVariable Long requestId,
        RedirectAttributes redirectAttributes) {

    try {
        projectService.rejectLeaveRequest(requestId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Leave request rejected.");

    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute(
                "errorMessage", e.getMessage());
    }

    return "redirect:/leave-requests";
}

}
