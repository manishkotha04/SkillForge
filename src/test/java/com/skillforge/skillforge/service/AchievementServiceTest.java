
package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.AchievementRequest;
import com.skillforge.skillforge.entity.Achievement;
import com.skillforge.skillforge.repository.AchievementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long ACHIEVEMENT_ID = 20L;

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AchievementService achievementService;

    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        currentUser = mock(AppUser.class);
    }

    private AchievementRequest validRequest() {
        AchievementRequest request = new AchievementRequest();
        request.setTitle("Java Certificate");
        request.setDescription("Completed Java certification.");
        request.setAchievedDate(LocalDate.of(2026, 10, 20));
        return request;
    }

    @Test
    void getMyAchievementsReturnsOnlyCurrentUsersAchievements() {

        List<Achievement> expected = List.of(new Achievement());

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);

        when(achievementRepository
                .findByUserIdOrderByAchievedDateDesc(USER_ID))
                .thenReturn(expected);

        List<Achievement> result =
                achievementService.getMyAchievements();

        assertSame(expected, result);

        verify(achievementRepository)
                .findByUserIdOrderByAchievedDateDesc(USER_ID);
    }

    @Test
    void getMyAchievementReturnsOwnedAchievement() {

        Achievement achievement = new Achievement();

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);

        when(achievementRepository.findByIdAndUserId(
                ACHIEVEMENT_ID, USER_ID))
                .thenReturn(Optional.of(achievement));

        Achievement result =
                achievementService.getMyAchievement(ACHIEVEMENT_ID);

        assertSame(achievement, result);
    }

    @Test
    void getMyAchievementRejectsAchievementNotOwnedByCurrentUser() {

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);

        when(achievementRepository.findByIdAndUserId(
                ACHIEVEMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> achievementService.getMyAchievement(ACHIEVEMENT_ID)
        );

        verify(achievementRepository, never())
                .delete(any(Achievement.class));
    }

    @Test
    void createAchievementAssignsCurrentUserAndSavesTrimmedFields() {

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        when(achievementRepository.save(any(Achievement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AchievementRequest request = validRequest();
        request.setTitle("  Java Certificate  ");
        request.setDescription("  Completed Java certification.  ");

        Achievement result =
                achievementService.createAchievement(request);

        ArgumentCaptor<Achievement> captor =
                ArgumentCaptor.forClass(Achievement.class);

        verify(achievementRepository).save(captor.capture());

        Achievement saved = captor.getValue();

        assertSame(currentUser, saved.getUser());
        assertEquals("Java Certificate", saved.getTitle());
        assertEquals("Completed Java certification.", saved.getDescription());
        assertEquals(LocalDate.of(2026, 10, 20), saved.getAchievedDate());

        assertSame(saved, result);
    }

    @Test
    void createAchievementRejectsBlankTitle() {

        AchievementRequest request = validRequest();
        request.setTitle("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> achievementService.createAchievement(request)
        );

        verifyNoInteractions(achievementRepository);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void createAchievementRejectsMissingDescription() {

        AchievementRequest request = validRequest();
        request.setDescription("");

        assertThrows(
                IllegalArgumentException.class,
                () -> achievementService.createAchievement(request)
        );

        verifyNoInteractions(achievementRepository);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void createAchievementRejectsMissingDate() {

        AchievementRequest request = validRequest();
        request.setAchievedDate(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> achievementService.createAchievement(request)
        );

        verifyNoInteractions(achievementRepository);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void updateAchievementUpdatesOwnedAchievement() {

        Achievement achievement = new Achievement();

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);

        when(achievementRepository.findByIdAndUserId(
                ACHIEVEMENT_ID, USER_ID))
                .thenReturn(Optional.of(achievement));

        when(achievementRepository.save(achievement))
                .thenReturn(achievement);

        AchievementRequest request = validRequest();
        request.setTitle("Updated Certificate");

        Achievement result =
                achievementService.updateAchievement(
                        ACHIEVEMENT_ID, request);

        assertEquals("Updated Certificate", achievement.getTitle());
        assertEquals(
                "Completed Java certification.",
                achievement.getDescription()
        );

        assertSame(achievement, result);

        verify(achievementRepository).save(achievement);
    }

    @Test
    void updateAchievementRejectsAnotherUsersAchievement() {

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);

        when(achievementRepository.findByIdAndUserId(
                ACHIEVEMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> achievementService.updateAchievement(
                        ACHIEVEMENT_ID, validRequest())
        );

        verify(achievementRepository, never())
                .save(any(Achievement.class));
    }

    @Test
    void deleteAchievementDeletesOwnedAchievement() {

        Achievement achievement = new Achievement();

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);

        when(achievementRepository.findByIdAndUserId(
                ACHIEVEMENT_ID, USER_ID))
                .thenReturn(Optional.of(achievement));

        achievementService.deleteAchievement(ACHIEVEMENT_ID);

        verify(achievementRepository).delete(achievement);
    }

    @Test
    void deleteAchievementRejectsAnotherUsersAchievement() {

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);

        when(achievementRepository.findByIdAndUserId(
                ACHIEVEMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> achievementService.deleteAchievement(ACHIEVEMENT_ID)
        );

        verify(achievementRepository, never())
                .delete(any(Achievement.class));
    }
}
