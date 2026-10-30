
package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.ProfileUpdateRequest;
import com.skillforge.skillforge.dto.ProfileViewDto;
import com.skillforge.skillforge.entity.Skill;
import com.skillforge.skillforge.entity.StudentProfile;
import com.skillforge.skillforge.entity.StudentSkill;
import com.skillforge.skillforge.repository.SkillRepository;
import com.skillforge.skillforge.repository.StudentProfileRepository;
import com.skillforge.skillforge.repository.StudentSkillRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long PROFILE_ID = 20L;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private StudentSkillRepository studentSkillRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ProfileService profileService;

    private AppUser currentUser;

    @BeforeEach
    void setUp() {

        currentUser = mock(AppUser.class);

        when(currentUser.getId()).thenReturn(USER_ID);

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);
    }

    private StudentProfile createProfile() {

        StudentProfile profile = mock(StudentProfile.class);

        when(profile.getId()).thenReturn(PROFILE_ID);

        when(profile.getDisplayName()).thenReturn("Test Student");
        when(profile.getBio()).thenReturn("Test bio");
        when(profile.getInterests()).thenReturn("Programming");

        return profile;
    }

    private StudentSkill createSkillLink(
            StudentProfile profile,
            String skillName) {

        Skill skill = new Skill();
        skill.setName(skillName);

        StudentSkill studentSkill = new StudentSkill();
        studentSkill.setStudentProfile(profile);
        studentSkill.setSkill(skill);

        return studentSkill;
    }

    @Test
    void getCurrentProfileReturnsExistingProfileAndSkills() {

        StudentProfile profile = createProfile();

        StudentSkill javaLink =
                createSkillLink(profile, "Java");

        StudentSkill sqlLink =
                createSkillLink(profile, "SQL");

        when(studentProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        when(studentSkillRepository
                .findByStudentProfileId(PROFILE_ID))
                .thenReturn(List.of(javaLink, sqlLink));

        ProfileViewDto result =
                profileService.getCurrentProfile();

        assertEquals("Test Student", result.getDisplayName());
        assertEquals("Test bio", result.getBio());
        assertEquals("Programming", result.getInterests());

        assertEquals(
                List.of("Java", "SQL"),
                result.getSkills()
        );
    }

    @Test
    void getCurrentProfileCreatesDefaultProfileWhenMissing() {

        when(studentProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty());

        when(currentUser.getFullName())
                .thenReturn("New Student");

        StudentProfile savedProfile = mock(StudentProfile.class);

        when(savedProfile.getId()).thenReturn(77L);
        when(savedProfile.getDisplayName()).thenReturn("New Student");
        when(savedProfile.getBio()).thenReturn("");
        when(savedProfile.getInterests()).thenReturn("");

        when(studentProfileRepository.save(any(StudentProfile.class)))
                .thenReturn(savedProfile);

        when(studentSkillRepository.findByStudentProfileId(77L))
                .thenReturn(List.of());

        ProfileViewDto result =
                profileService.getCurrentProfile();

        ArgumentCaptor<StudentProfile> captor =
                ArgumentCaptor.forClass(StudentProfile.class);

        verify(studentProfileRepository).save(captor.capture());

        StudentProfile createdProfile = captor.getValue();

        assertSame(currentUser, createdProfile.getUser());
        assertEquals("New Student", createdProfile.getDisplayName());
        assertEquals("", createdProfile.getBio());
        assertEquals("", createdProfile.getInterests());

        assertEquals("New Student", result.getDisplayName());
        assertTrue(result.getSkills().isEmpty());
    }

    @Test
    void updateCurrentProfileSavesDetailsAndNewSkills() {

        StudentProfile profile = createProfile();

        when(studentProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        when(studentProfileRepository.save(profile))
                .thenReturn(profile);

        // First call: no existing skills.
        // Second call: the newly saved skill for the DTO.
        when(studentSkillRepository
                .findByStudentProfileId(PROFILE_ID))
                .thenReturn(
                        List.of(),
                        List.of(createSkillLink(profile, "java"))
                );

        when(skillRepository.findByNameIgnoreCase("java"))
                .thenReturn(Optional.empty());

        when(skillRepository.save(any(Skill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(studentSkillRepository.save(any(StudentSkill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfileUpdateRequest request = new ProfileUpdateRequest();

        request.setDisplayName("Updated Student");
        request.setBio("Updated bio");
        request.setInterests("Java development");
        request.setSkills(List.of(" Java ", "JAVA", "", "   "));

        ProfileViewDto result =
                profileService.updateCurrentProfile(request);

        verify(profile).setDisplayName("Updated Student");
        verify(profile).setBio("Updated bio");
        verify(profile).setInterests("Java development");

        // Duplicate and differently capitalized skills
        // must create only one skill association.
        verify(skillRepository, times(1))
                .findByNameIgnoreCase("java");

        verify(skillRepository, times(1))
                .save(any(Skill.class));

        ArgumentCaptor<StudentSkill> captor =
                ArgumentCaptor.forClass(StudentSkill.class);

        verify(studentSkillRepository)
                .save(captor.capture());

        StudentSkill savedLink = captor.getValue();

        assertSame(profile, savedLink.getStudentProfile());
        assertEquals("java", savedLink.getSkill().getName());

        assertEquals(List.of("java"), result.getSkills());
    }

    @Test
    void updateCurrentProfileRemovesDeselectedSkills() {

        StudentProfile profile = createProfile();

        StudentSkill javaLink =
                createSkillLink(profile, "Java");

        StudentSkill sqlLink =
                createSkillLink(profile, "SQL");

        when(studentProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        when(studentProfileRepository.save(profile))
                .thenReturn(profile);

        // First call returns existing skills.
        // Second call returns the remaining skill.
        when(studentSkillRepository
                .findByStudentProfileId(PROFILE_ID))
                .thenReturn(
                        List.of(javaLink, sqlLink),
                        List.of(javaLink)
                );

        ProfileUpdateRequest request = new ProfileUpdateRequest();

        request.setDisplayName("Updated Student");
        request.setBio("Updated bio");
        request.setInterests("Programming");
        request.setSkills(List.of("Java"));

        ProfileViewDto result =
                profileService.updateCurrentProfile(request);

        verify(studentSkillRepository).delete(sqlLink);

        verify(studentSkillRepository, never())
                .delete(javaLink);

        verify(studentSkillRepository, never())
                .save(any(StudentSkill.class));

        verifyNoInteractions(skillRepository);

        assertEquals(
                List.of("Java"),
                result.getSkills()
        );
    }

    @Test
    void updateCurrentProfileClearsAllSkillsWhenNullProvided() {

        StudentProfile profile = createProfile();

        StudentSkill javaLink =
                createSkillLink(profile, "Java");

        StudentSkill sqlLink =
                createSkillLink(profile, "SQL");

        when(studentProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(profile));

        when(studentProfileRepository.save(profile))
                .thenReturn(profile);

        when(studentSkillRepository
                .findByStudentProfileId(PROFILE_ID))
                .thenReturn(
                        List.of(javaLink, sqlLink),
                        List.of()
                );

        ProfileUpdateRequest request = new ProfileUpdateRequest();

        request.setDisplayName("Updated Student");
        request.setBio("Updated bio");
        request.setInterests("Programming");
        request.setSkills(null);

        ProfileViewDto result =
                profileService.updateCurrentProfile(request);

        verify(studentSkillRepository).delete(javaLink);
        verify(studentSkillRepository).delete(sqlLink);

        assertTrue(result.getSkills().isEmpty());

        verifyNoInteractions(skillRepository);
    }
}
