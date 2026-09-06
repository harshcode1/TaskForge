package com.projectmgmttool.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persisted counterpart to TaskEvent (see dto/TaskEvent.java and
 * WebSocketConfig's Javadoc). The WebSocket broadcast is live-only —
 * miss the toast and it's gone. This table is the actual "activity feed /
 * audit log": every task create/update/delete gets a row here so a user
 * can open a project days later and see what happened, not just what
 * happened while they were looking. Written from the same call site as
 * the broadcast (TaskController#broadcast) so the two never drift apart.
 *
 * Deliberately flat like TaskEvent — projectId/taskId as plain UUID
 * columns rather than @ManyToOne relations — so a deleted task's history
 * survives the delete instead of cascading away or dangling a foreign key.
 */
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String type; // CREATED | UPDATED | DELETED

    private UUID taskId;

    private String taskTitle;

    private UUID projectId;

    private String actorName;

    private String actorEmail;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ActivityLog() {}

    public ActivityLog(String type, UUID taskId, String taskTitle, UUID projectId, String actorName, String actorEmail) {
        this.type = type;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.projectId = projectId;
        this.actorName = actorName;
        this.actorEmail = actorEmail;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
