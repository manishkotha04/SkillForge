package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.TaskCreateRequest;
import com.skillforge.skillforge.dto.TaskUpdateRequest;
import com.skillforge.skillforge.entity.Task;
import com.skillforge.skillforge.enums.TaskStatus;
import com.skillforge.skillforge.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public String listTasks(
            @RequestParam Long projectId,
            Model model) {

        model.addAttribute(
                "tasks",
                taskService.getTasksForProject(projectId));

        model.addAttribute("projectId", projectId);

        return "tasks";
    }

    @GetMapping("/new")
    public String newTask(
            @RequestParam Long projectId,
            Model model) {

        model.addAttribute(
                "task",
                new TaskCreateRequest());

        model.addAttribute("projectId", projectId);

        model.addAttribute(
        "members",
        taskService.getProjectMembers(projectId));

        return "task-form";
    }

    @PostMapping
    public String createTask(
            @RequestParam Long projectId,
            @Valid @ModelAttribute("task") TaskCreateRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("projectId", projectId);
            model.addAttribute(
                    "members",
                    taskService.getProjectMembers(projectId));
            return "task-form";
        }

        taskService.createTask(projectId, request);

        return "redirect:/tasks?projectId=" + projectId;
    }

    @GetMapping("/{id}/edit")
    public String editTask(
            @PathVariable Long id,
            Model model) {

        Task task = taskService.getTask(id);

        TaskUpdateRequest request = new TaskUpdateRequest();

        request.setTitle(task.getTitle());
        request.setDescription(task.getDescription());
        request.setDueDate(task.getDueDate());
        request.setStatus(task.getStatus());

        if (task.getAssignee() != null) {
            request.setAssigneeId(task.getAssignee().getId());
        }

        model.addAttribute("task", request);
        model.addAttribute(
                "projectId",
                task.getProject().getId());
        model.addAttribute("taskId", task.getId());

        model.addAttribute(
        "members",
        taskService.getProjectMembers(task.getProject().getId()));

        return "task-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateTask(
            @PathVariable Long id,
            @Valid @ModelAttribute("task") TaskUpdateRequest request,
            BindingResult bindingResult,
            Model model) {

        Task existingTask = taskService.getTask(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "projectId",
                    existingTask.getProject().getId());

            model.addAttribute("taskId", id);

            model.addAttribute(
                    "members",
                    taskService.getProjectMembers(
                            existingTask.getProject().getId()));

            return "task-edit";
        }

        taskService.updateTask(id, request);

        return "redirect:/tasks?projectId="
                + existingTask.getProject().getId();
    }

    @PostMapping("/{id}/status")
    public String updateTaskStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {

        Task task = taskService.updateTaskStatus(id, status);

        return "redirect:/tasks?projectId="
                + task.getProject().getId();
    }
}