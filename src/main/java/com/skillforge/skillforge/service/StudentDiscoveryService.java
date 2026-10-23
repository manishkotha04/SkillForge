package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.CurrentUserService;
import com.skillforge.skillforge.dto.StudentSummaryDto;
import com.skillforge.skillforge.entity.StudentProfile;
import com.skillforge.skillforge.entity.StudentSkill;
import com.skillforge.skillforge.repository.StudentProfileRepository;
import com.skillforge.skillforge.repository.StudentSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentDiscoveryService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final CurrentUserService currentUserService;

    public StudentDiscoveryService(
            StudentProfileRepository studentProfileRepository,
            StudentSkillRepository studentSkillRepository,
            CurrentUserService currentUserService) {

        this.studentProfileRepository = studentProfileRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.currentUserService = currentUserService;
    }

    public List<StudentSummaryDto> searchStudents(String query) {

        String searchTerm = query == null ? "" : query.trim();

        AppUser currentUser = currentUserService.getCurrentUser();

        List<StudentProfile> profiles = new ArrayList<>();

        if (searchTerm.isBlank()) {
            profiles.addAll(studentProfileRepository.findAll());
        } else {
            profiles.addAll(
                    studentProfileRepository
                            .findByDisplayNameContainingIgnoreCaseOrInterestsContainingIgnoreCase(
                                    searchTerm,
                                    searchTerm
                            )
            );

            List<StudentSkill> skillMatches =
                    studentSkillRepository.findBySkillNameContainingIgnoreCase(searchTerm);

            for (StudentSkill studentSkill : skillMatches) {
                StudentProfile profile = studentSkill.getStudentProfile();

                if (!profiles.contains(profile)) {
                    profiles.add(profile);
                }
            }
        }

        return profiles.stream()
                .filter(profile ->
                        !profile.getUser().getId().equals(currentUser.getId()))
                .map(this::toDto)
                .toList();
    }

    private StudentSummaryDto toDto(StudentProfile profile) {

        StudentSummaryDto dto = new StudentSummaryDto();

        dto.setId(profile.getId());
        dto.setDisplayName(profile.getDisplayName());
        dto.setBio(profile.getBio());
        dto.setInterests(profile.getInterests());

        return dto;
    }
}