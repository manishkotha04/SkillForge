
package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.AchievementRequest;
import com.skillforge.skillforge.entity.Achievement;
import com.skillforge.skillforge.service.AchievementService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("/profile/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    // Show the form to add an achievement
    @GetMapping("/new")
    public String showCreateForm(Model model) {

        model.addAttribute(
                "achievementRequest",
                new AchievementRequest());

        model.addAttribute("editing", false);

        return "achievement-form";
    }

    // Save a new achievement
    @PostMapping
    public String createAchievement(
            @ModelAttribute AchievementRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            achievementService.createAchievement(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Achievement added successfully.");

            return "redirect:/profile";

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "redirect:/profile/achievements/new";
        }
    }

    // Show the form to edit an existing achievement
    @GetMapping("/{achievementId}/edit")
    public String showEditForm(
            @PathVariable Long achievementId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Achievement achievement =
                    achievementService.getMyAchievement(achievementId);

            AchievementRequest request = new AchievementRequest();
            request.setTitle(achievement.getTitle());
            request.setDescription(achievement.getDescription());
            request.setAchievedDate(achievement.getAchievedDate());

            model.addAttribute("achievementRequest", request);
            model.addAttribute("achievementId", achievementId);
            model.addAttribute("editing", true);

            return "achievement-form";

        } catch (NoSuchElementException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Achievement not found or you are not authorized to access it.");

            return "redirect:/profile";
        }
    }

    // Update an existing achievement
    @PostMapping("/{achievementId}/edit")
    public String updateAchievement(
            @PathVariable Long achievementId,
            @ModelAttribute AchievementRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            achievementService.updateAchievement(
                    achievementId,
                    request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Achievement updated successfully.");

        } catch (IllegalArgumentException | NoSuchElementException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/profile";
    }

    // Delete an achievement
    @PostMapping("/{achievementId}/delete")
    public String deleteAchievement(
            @PathVariable Long achievementId,
            RedirectAttributes redirectAttributes) {

        try {
            achievementService.deleteAchievement(achievementId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Achievement deleted successfully.");

        } catch (NoSuchElementException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Achievement not found or you are not authorized to delete it.");
        }

        return "redirect:/profile";
    }
}
