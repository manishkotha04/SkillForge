package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.AchievementRequest;
import com.skillforge.skillforge.entity.Achievement;
import com.skillforge.skillforge.repository.AchievementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final CurrentUserService currentUserService;

    public AchievementService(
            AchievementRepository achievementRepository,
            CurrentUserService currentUserService) {

        this.achievementRepository = achievementRepository;
        this.currentUserService = currentUserService;
    }

    // Get achievements belonging to the logged-in student
    @Transactional(readOnly = true)
    public List<Achievement> getMyAchievements() {

        Long userId = currentUserService.getCurrentUserId();

        return achievementRepository
                .findByUserIdOrderByAchievedDateDesc(userId);
    }

    // Get one achievement only if it belongs to the logged-in student
    @Transactional(readOnly = true)
    public Achievement getMyAchievement(Long achievementId) {

        Long userId = currentUserService.getCurrentUserId();

        return achievementRepository
                .findByIdAndUserId(achievementId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Achievement not found."));
    }

    // Create an achievement for the logged-in student
    public Achievement createAchievement(AchievementRequest request) {

        validateRequest(request);

        AppUser currentUser = currentUserService.getCurrentUser();

        Achievement achievement = new Achievement();

        achievement.setUser(currentUser);
        achievement.setTitle(request.getTitle().trim());
        achievement.setDescription(request.getDescription().trim());
        achievement.setAchievedDate(request.getAchievedDate());

        return achievementRepository.save(achievement);
    }

    // Update only the logged-in student's achievement
    public Achievement updateAchievement(
            Long achievementId,
            AchievementRequest request) {

        validateRequest(request);

        Achievement achievement = getMyAchievement(achievementId);

        achievement.setTitle(request.getTitle().trim());
        achievement.setDescription(request.getDescription().trim());
        achievement.setAchievedDate(request.getAchievedDate());

        return achievementRepository.save(achievement);
    }

    // Delete only the logged-in student's achievement
    public void deleteAchievement(Long achievementId) {

        Achievement achievement = getMyAchievement(achievementId);

        achievementRepository.delete(achievement);
    }

    private void validateRequest(AchievementRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Achievement information is required.");
        }

        if (request.getTitle() == null
                || request.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Achievement title is required.");
        }

        if (request.getDescription() == null
                || request.getDescription().isBlank()) {
            throw new IllegalArgumentException(
                    "Achievement description is required.");
        }

        if (request.getAchievedDate() == null) {
            throw new IllegalArgumentException(
                    "Achievement date is required.");
        }
    }
}