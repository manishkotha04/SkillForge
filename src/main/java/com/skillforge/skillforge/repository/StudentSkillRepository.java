package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentSkillRepository extends JpaRepository<StudentSkill, Long> {

    List<StudentSkill> findByStudentProfileId(Long studentProfileId);

    List<StudentSkill> findBySkillNameContainingIgnoreCase(String name);
}