package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.PortfolioItemRequest;
import com.skillforge.skillforge.entity.PortfolioItem;
import com.skillforge.skillforge.service.PortfolioService;
import com.skillforge.skillforge.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final ProjectService projectService;

    public PortfolioController(
            PortfolioService portfolioService,
            ProjectService projectService) {
        this.portfolioService = portfolioService;
        this.projectService = projectService;
    }

    @GetMapping
    public String portfolio(Model model) {

        model.addAttribute(
                "items",
                portfolioService.getMyPortfolio());

        return "portfolio";
    }

    @GetMapping("/new")
    public String newPortfolioItem(Model model) {

        model.addAttribute(
                "item",
                new PortfolioItemRequest());

        model.addAttribute(
                "projects",
                projectService.getMyOwnedProjects());

        return "portfolio-form";
    }

    @PostMapping
    public String createPortfolioItem(
            @Valid @ModelAttribute("item")
            PortfolioItemRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "projects",
                    projectService.getMyOwnedProjects());

            return "portfolio-form";
        }

        portfolioService.createPortfolioItem(request);

        return "redirect:/portfolio";
    }

    @GetMapping("/{id}/edit")
    public String editPortfolioItem(
            @PathVariable Long id,
            Model model) {

        PortfolioItem item =
                portfolioService.getMyPortfolioItem(id);

        PortfolioItemRequest request =
                new PortfolioItemRequest();

        request.setTitle(item.getTitle());
        request.setDescription(item.getDescription());
        request.setExternalUrl(item.getExternalUrl());

        if (item.getProject() != null) {
            request.setProjectId(item.getProject().getId());
        }

        model.addAttribute("item", request);
        model.addAttribute("itemId", id);
        model.addAttribute(
                "projects",
                projectService.getMyOwnedProjects());

        return "portfolio-edit";
    }

    @PostMapping("/{id}/edit")
    public String updatePortfolioItem(
            @PathVariable Long id,
            @Valid @ModelAttribute("item")
            PortfolioItemRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "itemId",
                    id);

            model.addAttribute(
                    "projects",
                    projectService.getMyOwnedProjects());

            return "portfolio-edit";
        }

        portfolioService.updatePortfolioItem(id, request);

        return "redirect:/portfolio";
    }
}