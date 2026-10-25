package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.StudentSearchDto;
import com.skillforge.skillforge.dto.StudentSummaryDto;
import com.skillforge.skillforge.service.ProjectService;
import com.skillforge.skillforge.service.StudentDiscoveryService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/students")
public class StudentDiscoveryController {

    private final StudentDiscoveryService studentDiscoveryService;
    private final ProjectService projectService;

    public StudentDiscoveryController(
            StudentDiscoveryService studentDiscoveryService,
            ProjectService projectService) {

        this.studentDiscoveryService = studentDiscoveryService;
        this.projectService = projectService;
    }

    @GetMapping("/search")
    public String searchStudents(
            @RequestParam(required = false) Long projectId,
            @ModelAttribute("search") StudentSearchDto search,
            Model model) {

        List<StudentSummaryDto> students =
                studentDiscoveryService.searchStudents(
                        search.getQuery());

        model.addAttribute("students", students);
        model.addAttribute("projectId", projectId);

        boolean canInvite = false;

        if (projectId != null) {
            canInvite = projectService.isProjectOwner(projectId);
        }

        model.addAttribute("canInvite", canInvite);

        return "student-search";
    }
}