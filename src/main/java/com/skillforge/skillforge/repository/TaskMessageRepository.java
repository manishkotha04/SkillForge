package com.skillforge.skillforge.repository;

import com.skillforge.skillforge.entity.TaskMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskMessageRepository extends JpaRepository<TaskMessage, Long> {
    @EntityGraph(attributePaths = {"author"})
    List<TaskMessage> findByTask_IdOrderByCreatedAtAsc(Long taskId);
}
