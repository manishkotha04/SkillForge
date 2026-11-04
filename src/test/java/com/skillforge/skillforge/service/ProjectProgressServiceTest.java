package com.skillforge.skillforge.service;

import com.skillforge.skillforge.dto.ProjectProgressSummary;
import com.skillforge.skillforge.enums.TaskStatus;
import com.skillforge.skillforge.repository.TaskRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectProgressServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final ProjectProgressService service =
            new ProjectProgressService(taskRepository);

    @Test
    void projectWithNoTasksHasZeroProgress() {
        when(taskRepository.countByProject_Id(10L)).thenReturn(0L);
        when(taskRepository.countByProject_IdAndStatus(10L, TaskStatus.DONE))
                .thenReturn(0L);

        ProjectProgressSummary result = service.calculateProgress(10L);

        assertEquals(0L, result.getTotalTasks());
        assertEquals(0L, result.getCompletedTasks());
        assertEquals(0, result.getPercentage());
        verify(taskRepository).countByProject_Id(10L);
        verify(taskRepository).countByProject_IdAndStatus(10L, TaskStatus.DONE);
    }

    @Test
    void projectWithSomeDoneTasksGetsPartialProgress() {
        when(taskRepository.countByProject_Id(10L)).thenReturn(4L);
        when(taskRepository.countByProject_IdAndStatus(10L, TaskStatus.DONE))
                .thenReturn(2L);

        ProjectProgressSummary result = service.calculateProgress(10L);

        assertEquals(4L, result.getTotalTasks());
        assertEquals(2L, result.getCompletedTasks());
        assertEquals(50, result.getPercentage());
    }

    @Test
    void projectWithAllTasksDoneHasOneHundredPercentProgress() {
        when(taskRepository.countByProject_Id(10L)).thenReturn(3L);
        when(taskRepository.countByProject_IdAndStatus(10L, TaskStatus.DONE))
                .thenReturn(3L);

        ProjectProgressSummary result = service.calculateProgress(10L);

        assertEquals(3L, result.getTotalTasks());
        assertEquals(3L, result.getCompletedTasks());
        assertEquals(100, result.getPercentage());
    }

    @Test
    void percentageIsRoundedToNearestWholeNumber() {
        when(taskRepository.countByProject_Id(10L)).thenReturn(3L);
        when(taskRepository.countByProject_IdAndStatus(10L, TaskStatus.DONE))
                .thenReturn(1L);

        ProjectProgressSummary result = service.calculateProgress(10L);

        assertEquals(33, result.getPercentage());
    }
}
