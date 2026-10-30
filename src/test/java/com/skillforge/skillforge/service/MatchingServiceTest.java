
package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.StudentMatchDto;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.Skill;
import com.skillforge.skillforge.entity.StudentProfile;
import com.skillforge.skillforge.entity.StudentSkill;
import com.skillforge.skillforge.repository.StudentProfileRepository;
import com.skillforge.skillforge.repository.StudentSkillRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private StudentSkillRepository studentSkillRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MatchingService matchingService;


    private void prepareProject(String requiredSkills) {

        Project project = mock(Project.class);

        when(project.getRequiredSkillsText())
                .thenReturn(requiredSkills);

        when(projectService.getProjectForCurrentUser(1L))
                .thenReturn(project);

        AppUser currentUser = mock(AppUser.class);

        when(currentUser.getId())
                .thenReturn(100L);

        when(currentUserService.getCurrentUser())
                .thenReturn(currentUser);
    }


    private StudentProfile createStudent(
            Long userId,
            Long profileId,
            String displayName,
            String... skills) {

        AppUser user = mock(AppUser.class);

        when(user.getId())
                .thenReturn(userId);

        StudentProfile profile = mock(StudentProfile.class);

        when(profile.getId())
                .thenReturn(profileId);

        when(profile.getUser())
                .thenReturn(user);

        when(profile.getDisplayName())
                .thenReturn(displayName);

        when(profile.getBio())
                .thenReturn("Test student bio");

        when(profile.getInterests())
                .thenReturn("Programming");

        List<StudentSkill> studentSkills =
                java.util.Arrays.stream(skills)
                        .map(skillName -> {

                            Skill skill = mock(Skill.class);

                            when(skill.getName())
                                    .thenReturn(skillName);

                            StudentSkill studentSkill =
                                    mock(StudentSkill.class);

                            when(studentSkill.getSkill())
                                    .thenReturn(skill);

                            return studentSkill;
                        })
                        .toList();

        when(studentSkillRepository
                .findByStudentProfileId(profileId))
                .thenReturn(studentSkills);

        return profile;
    }


    @Test
    void calculatesCorrectMatchingPercentage() {

        prepareProject("Java, SQL, HTML");

        StudentProfile student = createStudent(
                200L,
                2L,
                "Alex",
                "Java",
                "SQL",
                "Python"
        );

        when(studentProfileRepository.findAll())
                .thenReturn(List.of(student));

        List<StudentMatchDto> results =
                matchingService.findMatchesForProject(1L);

        assertEquals(1, results.size());

        StudentMatchDto result = results.get(0);

        assertEquals(
                new BigDecimal("66.67"),
                result.getMatchPercentage()
        );

        assertEquals(
                List.of("Java", "SQL"),
                result.getMatchedSkills()
        );

        assertTrue(result.isMatchingAvailable());
    }


    @Test
    void handlesDuplicateAndCaseInsensitiveSkills() {

        prepareProject("Java, java, SQL, SQL, ");

        StudentProfile student = createStudent(
                200L,
                2L,
                "Alex",
                "java",
                "SQL",
                "sql"
        );

        when(studentProfileRepository.findAll())
                .thenReturn(List.of(student));

        List<StudentMatchDto> results =
                matchingService.findMatchesForProject(1L);

        StudentMatchDto result = results.get(0);

        assertEquals(
                new BigDecimal("100.00"),
                result.getMatchPercentage()
        );

        assertEquals(
                List.of("Java", "SQL"),
                result.getMatchedSkills()
        );
    }


    @Test
    void sortsStudentsByMatchingPercentage() {

        prepareProject("Java, SQL");

        StudentProfile zoey = createStudent(
                201L, 2L, "Zoey", "Java");

        StudentProfile ari = createStudent(
                202L, 3L, "Ari", "Java", "SQL");

        StudentProfile ben = createStudent(
                203L, 4L, "Ben", "SQL");

        when(studentProfileRepository.findAll())
                .thenReturn(List.of(zoey, ari, ben));

        List<StudentMatchDto> results =
                matchingService.findMatchesForProject(1L);

        assertEquals(3, results.size());

        assertEquals(
                "Ari",
                results.get(0).getStudent().getDisplayName()
        );

        assertEquals(
                new BigDecimal("100.00"),
                results.get(0).getMatchPercentage()
        );

        // Equal percentages should be ordered alphabetically.
        assertEquals(
                "Ben",
                results.get(1).getStudent().getDisplayName()
        );

        assertEquals(
                "Zoey",
                results.get(2).getStudent().getDisplayName()
        );
    }


   @Test
void excludesCurrentlyLoggedInStudent() {

    prepareProject("Java, SQL");

    // Create the current user's profile without stubbing
    // a skill lookup, because this profile should be skipped.
    AppUser currentStudentUser = mock(AppUser.class);

    when(currentStudentUser.getId())
            .thenReturn(100L);

    StudentProfile currentStudent = mock(StudentProfile.class);

    when(currentStudent.getUser())
            .thenReturn(currentStudentUser);

    StudentProfile anotherStudent = createStudent(
            200L, 2L, "Another Student", "Java");

    when(studentProfileRepository.findAll())
            .thenReturn(List.of(currentStudent, anotherStudent));

    List<StudentMatchDto> results =
            matchingService.findMatchesForProject(1L);

    assertEquals(1, results.size());

    assertEquals(
            "Another Student",
            results.get(0).getStudent().getDisplayName()
    );
}


    @Test
    void handlesProjectWithoutRequiredSkills() {

        prepareProject("   , , ");

        StudentProfile student = createStudent(
                200L, 2L, "Alex", "Java");

        when(studentProfileRepository.findAll())
                .thenReturn(List.of(student));

        List<StudentMatchDto> results =
                matchingService.findMatchesForProject(1L);

        assertEquals(1, results.size());

        StudentMatchDto result = results.get(0);

        assertFalse(result.isMatchingAvailable());

        assertNull(result.getMatchPercentage());

        assertTrue(result.getMatchedSkills().isEmpty());
    }


    @Test
    void includesStudentsWithZeroMatchingSkills() {

        prepareProject("Java, SQL");

        StudentProfile student = createStudent(
                200L, 2L, "Alex", "Python", "CSS");

        when(studentProfileRepository.findAll())
                .thenReturn(List.of(student));

        List<StudentMatchDto> results =
                matchingService.findMatchesForProject(1L);

        assertEquals(1, results.size());

        assertEquals(
                new BigDecimal("0.00"),
                results.get(0).getMatchPercentage()
        );

        assertTrue(
                results.get(0).getMatchedSkills().isEmpty()
        );
    }
}
