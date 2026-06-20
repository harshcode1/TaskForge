package com.projectmgmttool.backend.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public class CommentRequest {

    @NotNull(message = "Task ID is required")
    private UUID taskId;

    @NotBlank(message = "Comment text is required")
    @Size(min = 1, max = 1000, message = "Comment must be between 1 and 1000 characters")
    private String text;

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}