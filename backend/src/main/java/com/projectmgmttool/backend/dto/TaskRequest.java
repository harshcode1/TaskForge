package com.projectmgmttool.backend.dto;

import com.projectmgmttool.backend.entity.enums.Priority;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public class TaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private UUID projectId;

    private String assigneeEmail;

    @NotBlank(message = "Task status is required")
    @Pattern(regexp = "^(TODO|IN_PROGRESS|DONE|PENDING)$", message = "Status must be TODO, IN_PROGRESS, DONE, or PENDING")
    private String status;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private LocalDate dueDate;

    public TaskRequest() {}

    public TaskRequest(String title, String description, UUID projectId, String assigneeEmail, String status, Priority priority) {
        this.title = title;
        this.description = description;
        this.projectId = projectId;
        this.assigneeEmail = assigneeEmail;
        this.status = status;
        this.priority = priority;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getAssigneeEmail() { return assigneeEmail; }
    public void setAssigneeEmail(String assigneeEmail) { this.assigneeEmail = assigneeEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
