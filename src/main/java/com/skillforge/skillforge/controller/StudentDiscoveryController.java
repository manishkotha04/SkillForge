package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.StudentSearchDto;
import com.skillforge.skillforge.dto.StudentSummaryDto;
import com.skillforge.skillforge.service.StudentDiscoveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/students")
public class StudentDiscoveryController {

    private final StudentDiscoveryService studentDiscoveryService;

    public StudentDiscoveryController(
            StudentDiscoveryService studentDiscoveryService) {
        this.studentDiscoveryService = studentDiscoveryService;
    }

    @GetMapping("/search")
    public String searchStudents(
            @ModelAttribute("search") StudentSearchDto search,
            Model model) {

        List<StudentSummaryDto> students =
                studentDiscoveryService.searchStudents(search.getQuery());

        model.addAttribute("students", students);

        return "student-search";
    }
}