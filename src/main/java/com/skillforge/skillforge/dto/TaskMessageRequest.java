package com.skillforge.skillforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TaskMessageRequest {
    @NotBlank(message = "Enter a message before sending.")
    @Size(max = 2000, message = "Messages cannot exceed 2,000 characters.")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
