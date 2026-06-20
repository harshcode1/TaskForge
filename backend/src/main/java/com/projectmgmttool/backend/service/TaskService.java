package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.TaskRequest;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.ProjectMember;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.ProjectRepository;
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
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private UserRepository userRepository;

    public Task createTask(TaskRequest request, String creatorEmail) {
        if (request.getProjectId() == null) {
            throw new CustomApiException("Project ID is required to create a task", 400);
        }
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomApiException("Project not found", 404));

        User assignedTo = null;
        if (request.getAssigneeEmail() != null && !request.getAssigneeEmail().isBlank()) {
            assignedTo = userRepository.findByEmail(request.getAssigneeEmail())
                    .orElseThrow(() -> new CustomApiException("Assignee not found: " + request.getAssigneeEmail(), 404));
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

    public List<Task> getTasksForProject(UUID projectId, Optional<String> statusOpt) {
        String status = statusOpt.orElse(null);
        if (status != null && !status.isEmpty()) {
            return taskRepository.findByProjectIdAndStatus(projectId, status);
        }
        return taskRepository.findByProjectId(projectId);
    }

    public List<Task> getTasksForUser(String userEmail) {
        return taskRepository.findByAssigneeEmail(userEmail);
    }

    public Task updateTask(UUID taskId, TaskRequest request, String userEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomApiException("Task not found", 404));

        User requestingUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomApiException("User not found", 404));

        boolean isOwner = task.getProject().getOwner().getId().equals(requestingUser.getId());
        boolean isMember = projectMemberRepository
                .findByProjectIdAndUserEmail(task.getProject().getId(), userEmail).isPresent();

        if (!isOwner && !isMember) {
            throw new CustomApiException("You are not authorized to update this task", 403);
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.valueOf(request.getStatus()));
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        // Update assignee
        if (request.getAssigneeEmail() != null && !request.getAssigneeEmail().isBlank()
                && !request.getAssigneeEmail().equals("UNASSIGNED")) {
            User newAssignee = userRepository.findByEmail(request.getAssigneeEmail())
                    .orElseThrow(() -> new CustomApiException("Assignee not found: " + request.getAssigneeEmail(), 404));
            task.setAssignee(newAssignee);
        } else {
            task.setAssignee(null);
        }

        return taskRepository.save(task);
    }

    public void deleteTask(UUID taskId, String userEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomApiException("Task not found", 404));

        User requestingUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomApiException("User not found", 404));

        Project project = task.getProject();
        boolean isOwner = project.getOwner().getId().equals(requestingUser.getId());

        Optional<ProjectMember> memberOpt = projectMemberRepository
                .findByProjectIdAndUserEmail(project.getId(), userEmail);

        boolean isManager = memberOpt.isPresent() && memberOpt.get().getRole() == Role.MANAGER;

        if (!isOwner && !isManager) {
            throw new CustomApiException("You do not have permission to delete this task", 403);
        }

        taskRepository.delete(task);
    }
}
