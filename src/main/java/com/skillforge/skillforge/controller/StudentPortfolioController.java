package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.StudentSummaryDto;
import com.skillforge.skillforge.service.PortfolioService;
import com.skillforge.skillforge.service.StudentDiscoveryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/students")
public class StudentPortfolioController {

    private final StudentDiscoveryService studentDiscoveryService;
    private final PortfolioService portfolioService;

    public StudentPortfolioController(
            StudentDiscoveryService studentDiscoveryService,
            PortfolioService portfolioService) {
        this.studentDiscoveryService = studentDiscoveryService;
        this.portfolioService = portfolioService;
    }

    /**
     * Shows a read-only portfolio to authenticated SkillForge students.
     * Global Spring Security configuration requires authentication for this route.
     */
    @GetMapping("/{studentId}/portfolio")
    public String viewStudentPortfolio(
            @PathVariable Long studentId,
            Model model) {

        StudentSummaryDto student = studentDiscoveryService
                .findStudentSummaryByUserId(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student profile was not found."));

        model.addAttribute("student", student);
        model.addAttribute(
                "items", portfolioService.getPortfolioForStudent(studentId));
        return "student-portfolio";
    }
}
