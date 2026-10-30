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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class ProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final SkillRepository skillRepository;
    private final CurrentUserService currentUserService;

    public ProfileService(
            StudentProfileRepository studentProfileRepository,
            StudentSkillRepository studentSkillRepository,
            SkillRepository skillRepository,
            CurrentUserService currentUserService) {

        this.studentProfileRepository = studentProfileRepository;
        this.studentSkillRepository = studentSkillRepository;
        this.skillRepository = skillRepository;
        this.currentUserService = currentUserService;
    }

    public ProfileViewDto getCurrentProfile() {

        AppUser currentUser = currentUserService.getCurrentUser();

        StudentProfile profile = studentProfileRepository
                .findByUserId(currentUser.getId())
                .orElseGet(() -> createDefaultProfile(currentUser));

        return toViewDto(profile);
    }

    public ProfileViewDto updateCurrentProfile(ProfileUpdateRequest request) {

        AppUser currentUser = currentUserService.getCurrentUser();

        StudentProfile profile = studentProfileRepository
                .findByUserId(currentUser.getId())
                .orElseGet(() -> createDefaultProfile(currentUser));

        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setInterests(request.getInterests());

        studentProfileRepository.save(profile);

        updateSkills(profile, request.getSkills());

        return toViewDto(profile);
    }

    private void updateSkills(
            StudentProfile profile,
            List<String> requestedSkills) {

        List<StudentSkill> existingStudentSkills =
                studentSkillRepository.findByStudentProfileId(profile.getId());

        Set<String> requestedSkillNames = new HashSet<>();

        if (requestedSkills != null) {

            for (String skillName : requestedSkills) {

                if (skillName == null) {
                    continue;
                }

                String cleanedName = skillName.trim();

                if (!cleanedName.isEmpty()) {
                    requestedSkillNames.add(
                            cleanedName.toLowerCase(Locale.ROOT)
                    );
                }
            }
        }

        /*
         * Remove skills that are no longer selected.
         */
        for (StudentSkill studentSkill : existingStudentSkills) {

            String existingName = studentSkill.getSkill().getName()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (!requestedSkillNames.contains(existingName)) {
                studentSkillRepository.delete(studentSkill);
            }
        }

        /*
         * Find the skills that are already attached to this profile.
         */
        Set<String> existingSkillNames = new HashSet<>();

        for (StudentSkill studentSkill : existingStudentSkills) {

            String existingName = studentSkill.getSkill().getName()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (requestedSkillNames.contains(existingName)) {
                existingSkillNames.add(existingName);
            }
        }

        /*
         * Add only skills that do not already exist
         * for this student profile.
         */
        for (String requestedSkillName : requestedSkillNames) {

            if (existingSkillNames.contains(requestedSkillName)) {
                continue;
            }

            Skill skill = skillRepository
                    .findByNameIgnoreCase(requestedSkillName)
                    .orElseGet(() -> {
                        Skill newSkill = new Skill();
                        newSkill.setName(requestedSkillName);
                        return skillRepository.save(newSkill);
                    });

            StudentSkill studentSkill = new StudentSkill();
            studentSkill.setStudentProfile(profile);
            studentSkill.setSkill(skill);

            studentSkillRepository.save(studentSkill);
        }
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
                .map(Skill::getName)
                .toList();

        dto.setSkills(skillNames);

        return dto;
    }
}