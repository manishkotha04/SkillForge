
package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.StudentMatchDto;
import com.skillforge.skillforge.dto.StudentSummaryDto;
import com.skillforge.skillforge.entity.Project;
import com.skillforge.skillforge.entity.StudentProfile;
import com.skillforge.skillforge.entity.StudentSkill;
import com.skillforge.skillforge.repository.StudentProfileRepository;
import com.skillforge.skillforge.repository.StudentSkillRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class MatchingService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public MatchingService(
            StudentProfileRepository studentProfileRepository,
            StudentSkillRepository studentSkillRepository,
            ProjectService projectService,
            CurrentUserService currentUserService) {

        this.studentProfileRepository = studentProfileRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    public List<StudentMatchDto> findMatchesForProject(Long projectId) {

        // Verify the current user can access this project.
        Project project =
                projectService.getProjectForCurrentUser(projectId);

        // Extract distinct required skills.
        List<String> requiredSkills =
                getDistinctSkills(project.getRequiredSkillsText());

        AppUser currentUser = currentUserService.getCurrentUser();

        List<StudentMatchDto> results = new ArrayList<>();

        // Get all student profiles.
        List<StudentProfile> profiles =
                studentProfileRepository.findAll();

        for (StudentProfile profile : profiles) {

            // Do not recommend the currently logged-in user.
            if (profile.getUser().getId()
                    .equals(currentUser.getId())) {
                continue;
            }

            // Get this student's skills.
            List<StudentSkill> studentSkills =
                    studentSkillRepository.findByStudentProfileId(
                            profile.getId());

            Map<String, String> candidateSkillMap =
                    new LinkedHashMap<>();

            for (StudentSkill studentSkill : studentSkills) {

                String skillName =
                        studentSkill.getSkill().getName().trim();

                if (!skillName.isEmpty()) {
                    candidateSkillMap.putIfAbsent(
                            normalize(skillName),
                            skillName);
                }
            }

            // Build the existing student summary DTO.
            StudentSummaryDto student = new StudentSummaryDto();

            student.setId(profile.getUser().getId());
            student.setDisplayName(profile.getDisplayName());
            student.setBio(profile.getBio());
            student.setInterests(profile.getInterests());

            student.setSkills(
                    new ArrayList<>(candidateSkillMap.values()));

            // No project requirements: matching is unavailable.
            if (requiredSkills.isEmpty()) {

                results.add(new StudentMatchDto(
                        student,
                        null,
                        List.of(),
                        false));

                continue;
            }

            // Identify the skills shared with the project.
            List<String> matchedSkills = requiredSkills.stream()
                    .filter(required ->
                            candidateSkillMap.containsKey(
                                    normalize(required)))
                    .toList();

            // Calculate the matching percentage.
            BigDecimal percentage =
                    BigDecimal.valueOf(matchedSkills.size())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    BigDecimal.valueOf(
                                            requiredSkills.size()),
                                    2,
                                    RoundingMode.HALF_UP);

            results.add(new StudentMatchDto(
                    student,
                    percentage,
                    matchedSkills,
                    true));
        }

        // Sort by highest matching percentage first.
        if (!requiredSkills.isEmpty()) {

            results.sort(
                    Comparator.comparing(
                            StudentMatchDto::getMatchPercentage)
                            .reversed()
                            .thenComparing(
                                    match -> match.getStudent()
                                            .getDisplayName(),
                                    Comparator.nullsLast(
                                            String.CASE_INSENSITIVE_ORDER))
            );

        } else {

            // Stable alphabetical ordering when matching is unavailable.
            results.sort(
                    Comparator.comparing(
                            match -> match.getStudent().getDisplayName(),
                            Comparator.nullsLast(
                                    String.CASE_INSENSITIVE_ORDER))
            );
        }

        return results;
    }

    public boolean isMatchingAvailable(Long projectId) {

        Project project =
                projectService.getProjectForCurrentUser(projectId);

        return !getDistinctSkills(
                project.getRequiredSkillsText()).isEmpty();
    }

    private List<String> getDistinctSkills(String skillsText) {

        Map<String, String> uniqueSkills = new LinkedHashMap<>();

        if (skillsText == null || skillsText.isBlank()) {
            return List.of();
        }

        // Project required skills are comma-separated.
        for (String skill : skillsText.split(",")) {

            String cleanedSkill = skill.trim();

            if (!cleanedSkill.isEmpty()) {

                uniqueSkills.putIfAbsent(
                        normalize(cleanedSkill),
                        cleanedSkill);
            }
        }

        return new ArrayList<>(uniqueSkills.values());
    }

    private String normalize(String skill) {
        return skill.trim().toLowerCase(Locale.ROOT);
    }
}
