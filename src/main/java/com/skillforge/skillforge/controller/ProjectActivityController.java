package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.service.ProjectActivityService;
import com.skillforge.skillforge.service.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProjectActivityController {
    private final ProjectService projectService;
    private final ProjectActivityService activityService;

    public ProjectActivityController(ProjectService projectService,
                                     ProjectActivityService activityService) {
        this.projectService = projectService;
        this.activityService = activityService;
    }

    @GetMapping("/projects/{projectId}/activity")
    public String showActivity(@PathVariable Long projectId, Model model) {
        Project project = projectService.getProjectForCurrentUser(projectId);
        model.addAttribute("project", project);
        model.addAttribute("activities", activityService.getForProject(projectId));
        return "projects/activity";
    }
}
