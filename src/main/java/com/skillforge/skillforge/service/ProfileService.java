package com.skillforge.skillforge.service;

import com.skillforge.skillforge.AppUser;
import com.skillforge.skillforge.dto.ProfileUpdateRequest;
import com.skillforge.skillforge.dto.ProfileViewDto;
import com.skillforge.skillforge.entity.StudentProfile;
import com.skillforge.skillforge.entity.StudentSkill;
import com.skillforge.skillforge.repository.StudentProfileRepository;
import com.skillforge.skillforge.repository.StudentSkillRepository;
import com.skillforge.skillforge.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final CurrentUserService currentUserService;

    public ProfileService(
            StudentProfileRepository studentProfileRepository,
            StudentSkillRepository studentSkillRepository,
            CurrentUserService currentUserService) {

        this.studentProfileRepository = studentProfileRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ProfileViewDto getCurrentProfile() {
        AppUser currentUser = currentUserService.getCurrentUser();

        StudentProfile profile = studentProfileRepository
                .findByUserId(currentUser.getId())
                .orElseGet(() -> createDefaultProfile(currentUser));

        return toViewDto(profile);
    }

    @Transactional
    public ProfileViewDto updateCurrentProfile(ProfileUpdateRequest request) {
        AppUser currentUser = currentUserService.getCurrentUser();

        StudentProfile profile = studentProfileRepository
                .findByUserId(currentUser.getId())
                .orElseGet(() -> createDefaultProfile(currentUser));

        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setInterests(request.getInterests());

        studentProfileRepository.save(profile);

        return toViewDto(profile);
    }

    private StudentProfile createDefaultProfile(AppUser user) {
        StudentProfile profile = new StudentProfile();

        profile.setUser(user);
        profile.setDisplayName(user.getFullName());
        profile.setBio("");
        profile.setInterests("");

        return studentProfileRepository.save(profile);
    }

    private ProfileViewDto toViewDto(StudentProfile profile) {
        ProfileViewDto dto = new ProfileViewDto();

        dto.setId(profile.getId());
        dto.setDisplayName(profile.getDisplayName());
        dto.setBio(profile.getBio());
        dto.setInterests(profile.getInterests());

        List<String> skillNames = studentSkillRepository
                .findByStudentProfileId(profile.getId())
                .stream()
                .map(StudentSkill::getSkill)
                .map(skill -> skill.getName())
                .toList();

        dto.setSkills(skillNames);

        return dto;
    }
}