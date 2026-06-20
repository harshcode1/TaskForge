package com.projectmgmttool.backend.dto;

import com.projectmgmttool.backend.entity.enums.Priority;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class TaskDTO {
    private UUID id;
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private UUID projectId;
    private String assigneeEmail;
    private String assigneeName;
    private LocalDate dueDate;

    public TaskDTO() {}

    public TaskDTO(UUID id, String title, String description, Priority priority, TaskStatus status,
                   LocalDateTime createdAt, UUID projectId, String assigneeEmail, String assigneeName, LocalDate dueDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.projectId = projectId;
        this.assigneeEmail = assigneeEmail;
        this.assigneeName = assigneeName;
        this.dueDate = dueDate;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getAssigneeEmail() { return assigneeEmail; }
    public void setAssigneeEmail(String assigneeEmail) { this.assigneeEmail = assigneeEmail; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
