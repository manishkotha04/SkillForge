package com.skillforge.skillforge.controller;

import com.skillforge.skillforge.dto.TaskMessageRequest;
import com.skillforge.skillforge.entity.Task;
import com.skillforge.skillforge.service.TaskMessageService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TaskChatController {
    private final TaskMessageService taskMessageService;

    public TaskChatController(TaskMessageService taskMessageService) {
        this.taskMessageService = taskMessageService;
    }

    @GetMapping("/tasks/{taskId}/chat")
    public String showChat(@PathVariable Long taskId, Model model) {
        Task task = taskMessageService.getTaskForChat(taskId);
        model.addAttribute("task", task);
        model.addAttribute("messages", taskMessageService.getMessages(taskId));
        model.addAttribute("messageRequest", new TaskMessageRequest());
        return "task-chat";
    }

    @PostMapping("/tasks/{taskId}/chat")
    public String sendMessage(@PathVariable Long taskId,
                              @Valid @ModelAttribute("messageRequest") TaskMessageRequest request,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Task task = taskMessageService.getTaskForChat(taskId);
            model.addAttribute("task", task);
            model.addAttribute("messages", taskMessageService.getMessages(taskId));
            return "task-chat";
        }
        taskMessageService.sendMessage(taskId, request.getContent());
        redirectAttributes.addFlashAttribute("successMessage", "Message sent.");
        return "redirect:/tasks/" + taskId + "/chat";
    }
}
