package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @EntityGraph(attributePaths = {"assignee"})
    List<Task> findByProjectIdOrderByDueDateAsc(Long projectId);

    Optional<Task> findByIdAndProjectId(Long id, Long projectId);

    List<Task> findByAssigneeId(Long assigneeId);
}