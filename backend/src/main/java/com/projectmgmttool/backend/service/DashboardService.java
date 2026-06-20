package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.DashboardResponse;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.ProjectRepository;
import com.projectmgmttool.backend.repository.TaskRepository;
import com.projectmgmttool.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    public DashboardResponse getDashboardData(UUID projectId, String userEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomApiException("Project not found", 404));

        boolean isOwner = project.getOwner().getEmail().equals(userEmail);
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserEmail(projectId, userEmail);

        if (!isOwner && !isMember) {
            throw new CustomApiException("You are not authorized to view this project's dashboard", 403);
        }

        List<Task> tasks = taskRepository.findByProjectId(project.getId());
        return buildResponse(tasks, projectMemberRepository.findByProjectId(projectId).size() + 1);
    }

    public DashboardResponse getUserDashboardData(String userEmail) {
        userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomApiException("User not found", 404));

        List<Task> assignedTasks = taskRepository.findByAssigneeEmail(userEmail);

        Map<String, Long> taskStatusCounts = assignedTasks.stream()
                .filter(t -> t.getStatus() != null)
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        Map<String, Long> tasksPerProject = assignedTasks.stream()
                .filter(t -> t.getProject() != null)
                .collect(Collectors.groupingBy(t -> t.getProject().getName(), Collectors.counting()));

        DashboardResponse response = buildResponse(assignedTasks, 0);
        response.setTasksPerUser(tasksPerProject);
        return response;
    }

    private DashboardResponse buildResponse(List<Task> tasks, int totalMembers) {
        long total = tasks.size();
        long completed = tasks.stream().filter(t -> t.getStatus() != null && t.getStatus().name().equals("DONE")).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() != null && t.getStatus().name().equals("IN_PROGRESS")).count();
        long pending = tasks.stream().filter(t -> t.getStatus() != null && !t.getStatus().name().equals("DONE")).count();
        long overdue = tasks.stream().filter(t ->
                t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()) && !t.getStatus().name().equals("DONE")
        ).count();
        int completionRate = total > 0 ? (int) Math.round((completed * 100.0) / total) : 0;

        Map<String, Long> statusCounts = tasks.stream()
                .filter(t -> t.getStatus() != null)
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        Map<String, Long> perUser = tasks.stream()
                .filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignee().getEmail(), Collectors.counting()));

        DashboardResponse response = new DashboardResponse(statusCounts, perUser);
        response.setTotalTasks(total);
        response.setCompletedTasks(completed);
        response.setPendingTasks(pending);
        response.setInProgressTasks(inProgress);
        response.setCompletionRate(completionRate);
        response.setOverdueTasks(overdue);
        response.setTotalMembers(totalMembers);
        return response;
    }
}
