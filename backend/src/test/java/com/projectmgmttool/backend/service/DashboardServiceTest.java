package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.DashboardResponse;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.ProjectMember;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.TaskRepository;
import com.projectmgmttool.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private ProjectAccessService access;

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
    void getDashboardData_asMember_returnsSummary() {
        Task doneTask = new Task(); doneTask.setStatus(TaskStatus.DONE);
        Task todoTask = new Task(); todoTask.setStatus(TaskStatus.TODO);

        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(taskRepository.findByProjectId(projectId)).thenReturn(List.of(doneTask, todoTask));
        when(projectMemberRepository.findByProjectId(projectId)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboardData(projectId, "owner@example.com");

        assertNotNull(response);
        assertEquals(2, response.getTotalTasks());
        assertEquals(1, response.getCompletedTasks());
        assertEquals(50, response.getCompletionRate());
    }

    @Test
    void getDashboardData_totalMembers_isMemberRowsPlusTheOwner() {
        // The owner is Project.owner, not a row in project_members, hence the +1.
        List<ProjectMember> twoMembers = List.of(
                new ProjectMember(new User(), project, Role.MEMBER),
                new ProjectMember(new User(), project, Role.MANAGER));
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(taskRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(projectMemberRepository.findByProjectId(projectId)).thenReturn(twoMembers);

        DashboardResponse response = dashboardService.getDashboardData(projectId, "owner@example.com");

        assertEquals(3, response.getTotalMembers());
    }

    @Test
    void getDashboardData_accessCheckFails_errorPropagatesAndNothingIsQueried() {
        when(access.requireMember(projectId, "stranger@example.com"))
                .thenThrow(new CustomApiException(ProjectAccessService.NOT_FOUND_MESSAGE, 404));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> dashboardService.getDashboardData(projectId, "stranger@example.com"));

        assertEquals(404, ex.getErrorCode());
        verifyNoInteractions(taskRepository);
    }
}
