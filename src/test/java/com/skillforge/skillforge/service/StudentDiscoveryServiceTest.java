
package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.StudentSummaryDto;
import com.skillforge.skillforge.entity.Skill;
import com.skillforge.skillforge.entity.StudentProfile;
import com.skillforge.skillforge.entity.StudentSkill;
import com.skillforge.skillforge.repository.StudentProfileRepository;
import com.skillforge.skillforge.repository.StudentSkillRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentDiscoveryServiceTest {

    private static final Long CURRENT_USER_ID = 100L;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private StudentSkillRepository studentSkillRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private StudentDiscoveryService studentDiscoveryService;

    private AppUser currentUser;

    @BeforeEach
    void setUp() {

        currentUser = mock(AppUser.class);

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);
    }

    private StudentProfile createStudent(
            Long profileId,
            Long userId,
            String displayName,
            String bio,
            String interests) {

        AppUser user = mock(AppUser.class);

        when(user.getId()).thenReturn(userId);

        StudentProfile profile = mock(StudentProfile.class);

        when(profile.getId()).thenReturn(profileId);
        when(profile.getUser()).thenReturn(user);
        when(profile.getDisplayName()).thenReturn(displayName);
        when(profile.getBio()).thenReturn(bio);
        when(profile.getInterests()).thenReturn(interests);

        return profile;
    }

    private StudentSkill createSkillLink(
            StudentProfile profile,
            String skillName) {

        Skill skill = new Skill();
        skill.setName(skillName);

        StudentSkill link = new StudentSkill();
        link.setStudentProfile(profile);
        link.setSkill(skill);

        return link;
    }

    @Test
    void blankSearchReturnsAllOtherStudents() {

        when(currentUser.getId())
                .thenReturn(CURRENT_USER_ID);

        // Minimal current-user profile:
        // its other properties should never be accessed.
        AppUser currentStudentUser = mock(AppUser.class);

        when(currentStudentUser.getId())
                .thenReturn(CURRENT_USER_ID);

        StudentProfile currentStudent = mock(StudentProfile.class);

        when(currentStudent.getUser())
                .thenReturn(currentStudentUser);

        StudentProfile anotherStudent = createStudent(
                2L, 200L,
                "Alex", "Student bio", "Web development");

        StudentSkill javaLink =
                createSkillLink(anotherStudent, "Java");

        when(studentProfileRepository.findAll())
                .thenReturn(List.of(currentStudent, anotherStudent));

        when(studentSkillRepository.findByStudentProfileId(2L))
                .thenReturn(List.of(javaLink));

        List<StudentSummaryDto> results =
                studentDiscoveryService.searchStudents("   ");

        assertEquals(1, results.size());

        StudentSummaryDto result = results.get(0);

        assertEquals(200L, result.getId());
        assertEquals("Alex", result.getDisplayName());
        assertEquals("Student bio", result.getBio());
        assertEquals("Web development", result.getInterests());
        assertEquals(List.of("Java"), result.getSkills());

        verify(studentProfileRepository).findAll();

        verify(studentProfileRepository, never())
                .findByDisplayNameContainingIgnoreCaseOrInterestsContainingIgnoreCase(
                        anyString(), anyString());

        verify(studentSkillRepository, never())
                .findBySkillNameContainingIgnoreCase(anyString());

        verify(studentSkillRepository, never())
                .findByStudentProfileId(1L);
    }

    @Test
    void nullSearchReturnsAllOtherStudents() {

        when(currentUser.getId())
                .thenReturn(CURRENT_USER_ID);

        StudentProfile student = createStudent(
                2L, 200L,
                "Alex", "Student bio", "Programming");

        when(studentProfileRepository.findAll())
                .thenReturn(List.of(student));

        when(studentSkillRepository.findByStudentProfileId(2L))
                .thenReturn(List.of());

        List<StudentSummaryDto> results =
                studentDiscoveryService.searchStudents(null);

        assertEquals(1, results.size());
        assertEquals("Alex", results.get(0).getDisplayName());

        verify(studentProfileRepository).findAll();
    }

    @Test
    void searchCombinesNameInterestAndSkillMatchesWithoutDuplicates() {

        when(currentUser.getId())
                .thenReturn(CURRENT_USER_ID);

        StudentProfile nameMatch = createStudent(
                2L, 201L,
                "Java Developer", "Backend developer", "Coding");

        StudentProfile interestMatch = createStudent(
                3L, 202L,
                "Alex", "Student bio", "Java programming");

        StudentProfile skillMatch = createStudent(
                4L, 203L,
                "Sam", "Student bio", "Design");

        // The service trims the query but preserves its case.
        when(studentProfileRepository
                .findByDisplayNameContainingIgnoreCaseOrInterestsContainingIgnoreCase(
                        "Java", "Java"))
                .thenReturn(List.of(nameMatch, interestMatch));

        StudentSkill interestMatchLink =
                createSkillLink(interestMatch, "Java");

        StudentSkill skillMatchLink =
                createSkillLink(skillMatch, "JavaScript");

        when(studentSkillRepository
                .findBySkillNameContainingIgnoreCase("Java"))
                .thenReturn(List.of(
                        interestMatchLink,
                        skillMatchLink));

        when(studentSkillRepository.findByStudentProfileId(2L))
                .thenReturn(List.of());

        when(studentSkillRepository.findByStudentProfileId(3L))
                .thenReturn(List.of(interestMatchLink));

        when(studentSkillRepository.findByStudentProfileId(4L))
                .thenReturn(List.of(skillMatchLink));

        List<StudentSummaryDto> results =
                studentDiscoveryService.searchStudents(" Java ");

        assertEquals(3, results.size());

        assertEquals(
                List.of(201L, 202L, 203L),
                results.stream()
                        .map(StudentSummaryDto::getId)
                        .toList()
        );

        assertEquals(
                "Java Developer",
                results.get(0).getDisplayName()
        );

        assertEquals(
                List.of("Java"),
                results.get(1).getSkills()
        );

        assertEquals(
                List.of("JavaScript"),
                results.get(2).getSkills()
        );

        verify(studentProfileRepository)
                .findByDisplayNameContainingIgnoreCaseOrInterestsContainingIgnoreCase(
                        "Java", "Java");

        verify(studentSkillRepository)
                .findBySkillNameContainingIgnoreCase("Java");
    }

    @Test
    void skillSearchReturnsStudentUsingAppUserId() {

        when(currentUser.getId())
                .thenReturn(CURRENT_USER_ID);

        StudentProfile student = createStudent(
                55L, 250L,
                "Developer", "Backend developer", "Programming");

        StudentSkill javaLink =
                createSkillLink(student, "Java");

        when(studentProfileRepository
                .findByDisplayNameContainingIgnoreCaseOrInterestsContainingIgnoreCase(
                        "java", "java"))
                .thenReturn(List.of());

        when(studentSkillRepository
                .findBySkillNameContainingIgnoreCase("java"))
                .thenReturn(List.of(javaLink));

        when(studentSkillRepository.findByStudentProfileId(55L))
                .thenReturn(List.of(javaLink));

        List<StudentSummaryDto> results =
                studentDiscoveryService.searchStudents("java");

        assertEquals(1, results.size());

        StudentSummaryDto result = results.get(0);

        assertEquals(250L, result.getId());
        assertEquals("Developer", result.getDisplayName());
        assertEquals(List.of("Java"), result.getSkills());
    }

    @Test
    void searchReturnsEmptyListWhenNoStudentsMatch() {

        when(studentProfileRepository
                .findByDisplayNameContainingIgnoreCaseOrInterestsContainingIgnoreCase(
                        "unknown", "unknown"))
                .thenReturn(List.of());

        when(studentSkillRepository
                .findBySkillNameContainingIgnoreCase("unknown"))
                .thenReturn(List.of());

        List<StudentSummaryDto> results =
                studentDiscoveryService.searchStudents("unknown");

        assertTrue(results.isEmpty());
    }
}
