package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.StudentMatchDto;
import com.skillforge.skillforge.dto.StudentSearchDto;
import com.skillforge.skillforge.dto.StudentSummaryDto;
import com.skillforge.skillforge.service.MatchingService;
import com.skillforge.skillforge.service.ProjectService;
import com.skillforge.skillforge.service.StudentDiscoveryService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/students")
public class StudentDiscoveryController {

    private final StudentDiscoveryService studentDiscoveryService;
    private final MatchingService matchingService;
    private final ProjectService projectService;

    public StudentDiscoveryController(
            StudentDiscoveryService studentDiscoveryService,
            MatchingService matchingService,
            ProjectService projectService) {

        this.studentDiscoveryService = studentDiscoveryService;
        this.matchingService = matchingService;
        this.projectService = projectService;
    }

    @GetMapping("/search")
    public String searchStudents(
            @RequestParam(required = false) Long projectId,
            @ModelAttribute("search") StudentSearchDto search,
            Model model) {

        List<StudentMatchDto> studentMatches;
        boolean matchingAvailable = false;

        if (projectId != null) {

            studentMatches =
                    matchingService.findMatchesForProject(projectId);

            matchingAvailable =
                    matchingService.isMatchingAvailable(projectId);

            String query = search.getQuery();

            if (query != null && !query.isBlank()) {

                studentMatches = studentMatches.stream()
                        .filter(match ->
                                matchesSearch(match.getStudent(), query))
                        .toList();
            }

        } else {

            List<StudentSummaryDto> students =
                    studentDiscoveryService.searchStudents(
                            search.getQuery());

            studentMatches = students.stream()
                    .map(student -> new StudentMatchDto(
                            student,
                            null,
                            List.of(),
                            false))
                    .toList();
        }

        model.addAttribute("studentMatches", studentMatches);
        model.addAttribute("projectId", projectId);
        model.addAttribute("matchingAvailable", matchingAvailable);

        boolean canInvite =
                projectId != null
                        && projectService.isProjectOwner(projectId);

        model.addAttribute("canInvite", canInvite);

        return "student-search";
    }

    private boolean matchesSearch(
            StudentSummaryDto student,
            String query) {

        String searchTerm = query.trim().toLowerCase(Locale.ROOT);

        boolean nameMatches =
                contains(student.getDisplayName(), searchTerm);

        boolean interestMatches =
                contains(student.getInterests(), searchTerm);

        boolean skillMatches =
                student.getSkills() != null
                        && student.getSkills().stream()
                        .anyMatch(skill ->
                                contains(skill, searchTerm));

        return nameMatches || interestMatches || skillMatches;
    }

    private boolean contains(String value, String searchTerm) {
        return value != null
                && value.toLowerCase(Locale.ROOT)
                        .contains(searchTerm);
    }
}