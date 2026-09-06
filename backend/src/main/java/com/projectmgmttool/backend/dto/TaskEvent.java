package com.projectmgmttool.backend.dto;

import java.util.UUID;

/**
 * Broadcast over /topic/project/{projectId}/tasks whenever a task is
 * created, updated, or deleted. Deliberately thin — no task content, just
 * enough for a listening client to (a) know something changed and refetch,
 * and (b) show a live "Sam moved a task" style notification without a
 * separate persisted activity-log entity. A client ignores its own actorEmail
 * (it already has the up-to-date state from the REST response that caused
 * this event) and only reacts to events from other users.
 */
public class TaskEvent {
    private String type; // CREATED | UPDATED | DELETED
    private UUID taskId;
    private String taskTitle;
    private UUID projectId;
    private String actorName;
    private String actorEmail;

    public TaskEvent() {}

    public TaskEvent(String type, UUID taskId, String taskTitle, UUID projectId, String actorName, String actorEmail) {
        this.type = type;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.projectId = projectId;
        this.actorName = actorName;
        this.actorEmail = actorEmail;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
}
