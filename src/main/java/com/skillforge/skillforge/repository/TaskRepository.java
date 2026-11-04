package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @EntityGraph(attributePaths = {"assignee", "assignedUsers", "completedBy", "project"})
    List<Task> findByProjectIdOrderByDueDateAsc(Long projectId);

    @EntityGraph(attributePaths = {"assignee", "assignedUsers", "completedBy", "project"})
    Optional<Task> findById(Long id);

    Optional<Task> findByIdAndProjectId(Long id, Long projectId);
    List<Task> findByAssigneeId(Long assigneeId);
    long countByProject_Id(Long projectId);

    long countByProject_IdAndStatus(
        Long projectId,
        com.skillforge.skillforge.enums.TaskStatus status
    );
}
