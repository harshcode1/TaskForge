package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.TaskRequest;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.TaskRepository;
import com.projectmgmttool.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectAccessService projectAccessService;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private UserRepository userRepository;

    public Task createTask(TaskRequest request, String creatorEmail) {
        if (request.getProjectId() == null) {
            throw new CustomApiException("Project ID is required to create a task", 400);
        }
        Project project = projectAccessService.requireMember(request.getProjectId(), creatorEmail);

        User assignedTo = null;
        if (request.getAssigneeEmail() != null && !request.getAssigneeEmail().isBlank()) {
            assignedTo = findAssignee(request.getAssigneeEmail(), project);
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.valueOf(request.getStatus()));
        task.setDueDate(request.getDueDate());
        task.setProject(project);
        task.setAssignee(assignedTo);
        task.setCreatedAt(LocalDateTime.now());
        task.setPriority(request.getPriority());

        return taskRepository.save(task);
    }

    public List<Task> getTasksForProject(UUID projectId, Optional<String> statusOpt, String userEmail) {
        projectAccessService.requireMember(projectId, userEmail);

        String status = statusOpt.orElse(null);
        if (status != null && !status.isEmpty()) {
            return taskRepository.findByProjectIdAndStatus(projectId, parseStatus(status));
        }
        return taskRepository.findByProjectId(projectId);
    }

    public List<Task> getTasksForUser(String userEmail) {
        return taskRepository.findByAssigneeEmail(userEmail);
    }

    public Task updateTask(UUID taskId, TaskRequest request, String userEmail) {
        Task task = findTask(taskId);
        Project project = projectAccessService.requireMember(task.getProject().getId(), userEmail);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.valueOf(request.getStatus()));
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        // Update assignee
        if (request.getAssigneeEmail() != null && !request.getAssigneeEmail().isBlank()
                && !request.getAssigneeEmail().equals("UNASSIGNED")) {
            task.setAssignee(findAssignee(request.getAssigneeEmail(), project));
        } else {
            task.setAssignee(null);
        }

        return taskRepository.save(task);
    }

    public Task deleteTask(UUID taskId, String userEmail) {
        Task task = findTask(taskId);
        projectAccessService.requireManagerOrOwner(task.getProject().getId(), userEmail);

        taskRepository.delete(task);
        return task;
    }

    private TaskStatus parseStatus(String status) {
        try {
            return TaskStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CustomApiException("Invalid status: " + status, 400);
        }
    }

    // A missing task and a task in someone else's project give the same 404.
    private Task findTask(UUID taskId) {
        return taskRepository.findById(taskId).orElseThrow(ProjectAccessService::notFound);
    }

    // Tasks can only be assigned to people who are in the project (owner or member).
    private User findAssignee(String assigneeEmail, Project project) {
        User assignee = userRepository.findByEmail(assigneeEmail)
                .orElseThrow(() -> new CustomApiException("Assignee not found: " + assigneeEmail, 404));

        boolean isOwner = project.getOwner().getId().equals(assignee.getId());
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), assignee.getId());
        if (!isOwner && !isMember) {
            throw new CustomApiException("Assignee must be a member of this project", 400);
        }
        return assignee;
    }
}
