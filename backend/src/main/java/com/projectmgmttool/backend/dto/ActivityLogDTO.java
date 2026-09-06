package com.projectmgmttool.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ActivityLogDTO {
    private UUID id;
    private String type;
    private UUID taskId;
    private String taskTitle;
    private String actorName;
    private LocalDateTime createdAt;

    public ActivityLogDTO(UUID id, String type, UUID taskId, String taskTitle, String actorName, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.actorName = actorName;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getType() { return type; }
    public UUID getTaskId() { return taskId; }
    public String getTaskTitle() { return taskTitle; }
    public String getActorName() { return actorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
