
package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.ProjectMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, Long> {

    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);

    long countByProject_Id(Long projectId);

    @EntityGraph(attributePaths = {"user"})
    List<ProjectMember> findByProject_Id(Long projectId);

    @EntityGraph(attributePaths = {"project"})
    List<ProjectMember> findByUser_Id(Long userId);
}
