package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    List<StudentProfile> findByDisplayNameContainingIgnoreCaseOrInterestsContainingIgnoreCase(
            String displayName,
            String interests
    );
}