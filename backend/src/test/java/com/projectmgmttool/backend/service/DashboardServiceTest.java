package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.DashboardResponse;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.ProjectRepository;
import com.projectmgmttool.backend.repository.TaskRepository;
import com.projectmgmttool.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    private User owner;
    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setRole(Role.MANAGER);

        project = new Project();
        project.setId(projectId);
        project.setOwner(owner);
    }

    @Test
    void getDashboardData_asOwner_returnsSummary() {
        Task doneTask = new Task(); doneTask.setStatus(TaskStatus.DONE);
        Task todoTask = new Task(); todoTask.setStatus(TaskStatus.TODO);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserEmail(projectId, "owner@example.com")).thenReturn(false);
        when(taskRepository.findByProjectId(projectId)).thenReturn(List.of(doneTask, todoTask));
        when(projectMemberRepository.findByProjectId(projectId)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboardData(projectId, "owner@example.com");

        assertNotNull(response);
        assertEquals(2, response.getTotalTasks());
        assertEquals(1, response.getCompletedTasks());
        assertEquals(50, response.getCompletionRate());
    }

    @Test
    void getDashboardData_projectNotFound_throwsException() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(CustomApiException.class,
                () -> dashboardService.getDashboardData(projectId, "owner@example.com"));
    }

    @Test
    void getDashboardData_unauthorized_throwsForbidden() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserEmail(projectId, "stranger@example.com")).thenReturn(false);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> dashboardService.getDashboardData(projectId, "stranger@example.com"));
        assertEquals(403, ex.getErrorCode());
    }
}
