package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.ProfileUpdateRequest;
import com.skillforge.skillforge.dto.ProfileViewDto;
import com.skillforge.skillforge.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String viewProfile(Model model) {
        ProfileViewDto profile = profileService.getCurrentProfile();

        model.addAttribute("profile", profile);

        return "profile";
    }

    @GetMapping("/edit")
    public String editProfile(Model model) {
        ProfileViewDto profile = profileService.getCurrentProfile();

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setDisplayName(profile.getDisplayName());
        request.setBio(profile.getBio());
        request.setInterests(profile.getInterests());

        model.addAttribute("profile", request);

        return "profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
            @ModelAttribute ProfileUpdateRequest request) {

        profileService.updateCurrentProfile(request);

        return "redirect:/profile";
    }
}